package com.dsp.chat.api;

import java.util.Set;

public record PresenceResponse(String roomId, int count, Set<String> members) {
}
