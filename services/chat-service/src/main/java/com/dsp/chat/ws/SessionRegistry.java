package com.dsp.chat.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks WebSocket sessions connected to <em>this</em> service instance, keyed by room.
 *
 * <p>This registry is intentionally local/in-memory: with N replicas behind a load
 * balancer and no sticky sessions, any given instance only ever holds a slice of a
 * room's total membership. Cross-instance delivery is handled separately by the Redis
 * Pub/Sub fanout (see {@link com.dsp.chat.redis.ChatFanoutListener}), which every
 * instance subscribes to and uses to forward messages to whichever local sessions it
 * holds for a room. That split is what lets this design scale horizontally to
 * 10k+ concurrent sockets instead of being limited to what a single JVM can hold.
 */
@Component
public class SessionRegistry {

    private final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    public void add(String roomId, WebSocketSession session) {
        roomSessions.computeIfAbsent(roomId, r -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void remove(String roomId, WebSocketSession session) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        // Best-effort cleanup of empty room entries; a benign race with a concurrent
        // add() just means the map entry gets recreated, which is harmless.
        if (sessions.isEmpty()) {
            roomSessions.remove(roomId, sessions);
        }
    }

    public Set<WebSocketSession> sessionsFor(String roomId) {
        return roomSessions.getOrDefault(roomId, Set.of());
    }
}
