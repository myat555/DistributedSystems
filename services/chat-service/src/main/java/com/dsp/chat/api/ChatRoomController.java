package com.dsp.chat.api;

import com.dsp.chat.redis.ChatRedisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * Read-side REST endpoints that complement the WebSocket path: recent history for a
 * client that just joined a room, and cluster-wide presence for that room.
 */
@RestController
@RequestMapping("/api/chat/rooms")
public class ChatRoomController {

    private final ChatRedisService chatRedisService;

    public ChatRoomController(ChatRedisService chatRedisService) {
        this.chatRedisService = chatRedisService;
    }

    @GetMapping("/{roomId}/messages")
    public List<String> messages(@PathVariable String roomId,
                                  @RequestParam(defaultValue = "50") int limit) {
        return chatRedisService.history(roomId, limit);
    }

    @GetMapping("/{roomId}/presence")
    public PresenceResponse presence(@PathVariable String roomId) {
        Set<String> members = chatRedisService.presence(roomId);
        return new PresenceResponse(roomId, members.size(), members);
    }
}
