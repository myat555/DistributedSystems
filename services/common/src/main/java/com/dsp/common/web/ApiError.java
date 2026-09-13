package com.dsp.common.web;

import java.time.Instant;

/**
 * Uniform error body returned by every service's exception handlers,
 * so clients (and the API gateway) can parse errors consistently.
 */
public record ApiError(Instant timestamp, int status, String error, String message, String path) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path);
    }
}
