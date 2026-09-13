package com.dsp.chat.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Shape of the small JSON payload exchanged over the chat WebSocket, e.g.
 * {@code {"sender": "alice", "text": "hi"}}. Used only to validate that an
 * inbound frame is well-formed JSON matching this shape; the raw JSON text
 * itself (not this deserialized form) is what actually gets fanned out to
 * Redis, Kafka and history, so unknown extra fields a client sends are
 * preserved end-to-end.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatMessage(String sender, String text) {
}
