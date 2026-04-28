package ru.yandex.practicum.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${payment.initial-balance:100000}")
    private Long initialBalance;

    @Bean
    public ReactiveRedisTemplate<String, Long> reactiveRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Long> valueSerializer = new Jackson2JsonRedisSerializer<>(Long.class);

        RedisSerializationContext<String, Long> serializationContext = RedisSerializationContext
                .<String, Long>newSerializationContext()
                .key(keySerializer)
                .value(valueSerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }

    @Bean
    public Long initialBalance() {
        return initialBalance;
    }
}
