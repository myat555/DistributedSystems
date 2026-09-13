package com.dsp.chat.ws;

import com.dsp.chat.domain.ChatMessage;
import com.dsp.chat.kafka.ChatMessageProducer;
import com.dsp.chat.redis.ChatRedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Raw (non-STOMP, non-SockJS) WebSocket endpoint, one connection per chat participant,
 * registered at {@code /ws/chat/{roomId}} (see {@link com.dsp.chat.config.WebSocketConfig}).
 *
 * <p>Each inbound message triggers three independent things: a Redis Pub/Sub publish
 * for low-latency cross-instance fanout, an async Kafka send for durable history/replay,
 * and a bounded Redis list push for "recent context" REST reads. None of these block
 * each other and none block the WebSocket read loop.
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private static final String ROOM_ID_ATTR = "roomId";

    private final SessionRegistry sessionRegistry;
    private final ChatRedisService chatRedisService;
    private final ChatMessageProducer chatMessageProducer;
    private final ObjectMapper objectMapper;

    public ChatWebSocketHandler(SessionRegistry sessionRegistry,
                                 ChatRedisService chatRedisService,
                                 ChatMessageProducer chatMessageProducer,
                                 ObjectMapper objectMapper) {
        this.sessionRegistry = sessionRegistry;
        this.chatRedisService = chatRedisService;
        this.chatMessageProducer = chatMessageProducer;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String roomId = roomIdOf(session);
        sessionRegistry.add(roomId, session);
        chatRedisService.addPresence(roomId, session.getId());
        log.info("Session {} joined room {}", session.getId(), roomId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String roomId = roomIdOf(session);
        String payload = message.getPayload();

        if (!isWellFormed(payload)) {
            session.sendMessage(new TextMessage(
                    "{\"error\":\"invalid message, expected JSON like {\\\"sender\\\":\\\"alice\\\",\\\"text\\\":\\\"hi\\\"}\"}"));
            return;
        }

        // Low-latency fanout: publish the raw payload to this room's Redis Pub/Sub
        // channel. Every instance of this service (not just this one) is subscribed
        // to chat:* and will push the payload out to whichever local WebSocket
        // sessions it holds for this room. That decoupling - "who received the
        // message" vs. "who holds the recipients' sockets" - is what lets the
        // service scale horizontally across many replicas/pods to 10k+ concurrent
        // connections behind a load balancer with no sticky-session requirement.
        chatRedisService.publish(roomId, payload);

        // Durable, replayable audit trail. Kept fully separate from the fanout path
        // above so a slow or unavailable Kafka broker never adds latency to live
        // delivery. Keyed by roomId so partition-level ordering is per-room.
        chatMessageProducer.send(roomId, payload);

        // Bounded recent-history cache so a client that just joined can fetch context
        // via REST (GET /api/chat/rooms/{roomId}/messages) without replaying Kafka.
        chatRedisService.appendHistory(roomId, payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String roomId = roomIdOf(session);
        sessionRegistry.remove(roomId, session);
        chatRedisService.removePresence(roomId, session.getId());
        log.info("Session {} left room {} ({})", session.getId(), roomId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("Transport error on session {} in room {}: {}", session.getId(), roomIdOf(session), exception.getMessage());
    }

    private boolean isWellFormed(String payload) {
        try {
            objectMapper.readValue(payload, ChatMessage.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String roomIdOf(WebSocketSession session) {
        Object roomId = session.getAttributes().get(ROOM_ID_ATTR);
        return roomId == null ? "unknown" : roomId.toString();
    }
}
