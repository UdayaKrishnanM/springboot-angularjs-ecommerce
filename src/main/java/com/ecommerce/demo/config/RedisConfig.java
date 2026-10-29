package com.ecommerce.demo.config;

// CHANGE 6: Redis caching configuration.
// Redis is an in-memory store used here as a cache.
// When getProductById() is called, the result is stored in Redis for 10 minutes.
// The next call with the same ID returns the cached result WITHOUT touching MySQL.
// This dramatically reduces DB load for product browsing — the most-read endpoint.
//
// @EnableCaching activates Spring's caching proxy on all @Cacheable methods.

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))          // Cache entries expire after 10 min
                .disableCachingNullValues()                 // Don't cache null results
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()
                        )
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
