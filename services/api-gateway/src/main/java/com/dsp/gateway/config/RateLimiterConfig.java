package com.dsp.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class RateLimiterConfig {

    /**
     * Resolves the RequestRateLimiter key: prefer the caller's authenticated
     * identity (X-User-Id, set by JwtAuthenticationFilter once a token is
     * validated) and fall back to the client's remote address for
     * unauthenticated/best-effort limiting.
     */
    @Bean
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just(userId);
            }
            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            String host = remoteAddress != null ? remoteAddress.getHostString() : "unknown";
            return Mono.just(host);
        };
    }
}
