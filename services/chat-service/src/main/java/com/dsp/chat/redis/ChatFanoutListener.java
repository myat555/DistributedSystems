package com.dsp.chat.redis;

import com.dsp.chat.ws.SessionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Subscribed (via {@code RedisMessageListenerContainer}, see
 * {@link com.dsp.chat.config.RedisConfig}) to the {@code chat:*} Pub/Sub pattern.
 *
 * <p>Every instance of this service receives every message published to every room,
 * regardless of which instance originally accepted the sender's WebSocket connection.
 * Each instance then forwards the message only to the local sessions it actually
 * holds for that room (via {@link SessionRegistry}). This is the mechanism that lets
 * chat fan out correctly across N horizontally-scaled replicas sitting behind a load
 * balancer with no sticky sessions - the piece that makes 10k+ concurrent users
 * practical without every socket needing to live on one JVM.
 */
@Component
public class ChatFanoutListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(ChatFanoutListener.class);
    private static final String CHANNEL_PREFIX = "chat:";

    private final SessionRegistry sessionRegistry;

    public ChatFanoutListener(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        if (!channel.startsWith(CHANNEL_PREFIX)) {
            return;
        }
        String roomId = channel.substring(CHANNEL_PREFIX.length());
        TextMessage textMessage = new TextMessage(new String(message.getBody(), StandardCharsets.UTF_8));

        for (WebSocketSession session : sessionRegistry.sessionsFor(roomId)) {
            if (!session.isOpen()) {
                sessionRegistry.remove(roomId, session);
                continue;
            }
            try {
                session.sendMessage(textMessage);
            } catch (IOException e) {
                log.warn("Failed to forward message to session {} in room {}; removing it", session.getId(), roomId, e);
                sessionRegistry.remove(roomId, session);
            }
        }
    }
}
