package com.dsp.chat.config;

import com.dsp.chat.redis.ChatFanoutListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Wires up the Redis Pub/Sub subscription used for cross-instance chat fanout.
 * Every instance of this service subscribes to the same {@code chat:*} pattern,
 * so a message published by any one instance is delivered to all of them.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory,
                                                                         ChatFanoutListener chatFanoutListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(chatFanoutListener, new PatternTopic("chat:*"));
        return container;
    }
}
