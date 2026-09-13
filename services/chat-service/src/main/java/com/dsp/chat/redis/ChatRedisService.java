package com.dsp.chat.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * All Redis access for chat: cross-instance Pub/Sub fanout, cluster-wide presence
 * (a Set per room), and a bounded recent-history list per room.
 */
@Component
public class ChatRedisService {

    private static final String CHANNEL_PREFIX = "chat:";
    private static final String PRESENCE_KEY_PREFIX = "chat:presence:";
    private static final String HISTORY_KEY_PREFIX = "chat:history:";
    private static final int HISTORY_LIMIT = 200;

    private final StringRedisTemplate redis;

    public ChatRedisService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Publishes the raw message JSON to this room's Redis Pub/Sub channel. Every
     * instance of this service is subscribed to the {@code chat:*} pattern and
     * forwards the payload to whichever local sessions it holds for the room -
     * this is the low-latency cross-instance fanout that makes horizontal scaling
     * to 10k+ concurrent sockets possible without sticky sessions.
     */
    public void publish(String roomId, String payloadJson) {
        redis.convertAndSend(CHANNEL_PREFIX + roomId, payloadJson);
    }

    public void addPresence(String roomId, String sessionId) {
        redis.opsForSet().add(PRESENCE_KEY_PREFIX + roomId, sessionId);
    }

    public void removePresence(String roomId, String sessionId) {
        redis.opsForSet().remove(PRESENCE_KEY_PREFIX + roomId, sessionId);
    }

    public Set<String> presence(String roomId) {
        Set<String> members = redis.opsForSet().members(PRESENCE_KEY_PREFIX + roomId);
        return members == null ? Set.of() : members;
    }

    /** Appends to the room's recent-history list, trimmed to the most recent {@value #HISTORY_LIMIT} entries. */
    public void appendHistory(String roomId, String payloadJson) {
        String key = HISTORY_KEY_PREFIX + roomId;
        redis.opsForList().leftPush(key, payloadJson);
        redis.opsForList().trim(key, 0, HISTORY_LIMIT - 1);
    }

    public List<String> history(String roomId, int limit) {
        int cappedLimit = Math.min(Math.max(limit, 1), HISTORY_LIMIT);
        List<String> entries = redis.opsForList().range(HISTORY_KEY_PREFIX + roomId, 0, cappedLimit - 1L);
        return entries == null ? List.of() : entries;
    }
}
