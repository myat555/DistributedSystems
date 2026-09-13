package com.dsp.chat.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plain {@code WebSocketHandler} registrations don't get Spring MVC-style
 * {@code @DestinationVariable} path-variable binding, so this interceptor pulls the
 * {@code roomId} out of the handshake URI (e.g. {@code /ws/chat/room1} -> {@code room1})
 * and puts it into the handshake attributes map. Spring's default handshake handler
 * copies that attributes map onto the resulting {@code WebSocketSession}, so handlers
 * can read it back via {@code session.getAttributes().get("roomId")}.
 */
public class RoomIdHandshakeInterceptor implements HandshakeInterceptor {

    private static final Pattern ROOM_ID_PATTERN = Pattern.compile("/ws/chat/([^/]+)/?$");

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Matcher matcher = ROOM_ID_PATTERN.matcher(request.getURI().getPath());
        if (!matcher.find()) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }
        attributes.put("roomId", matcher.group(1));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
