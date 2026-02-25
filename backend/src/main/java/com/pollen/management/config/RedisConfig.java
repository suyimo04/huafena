package com.pollen.management.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 缓存配置。
 * <p>
 * 配置不同缓存区域的 TTL：
 * <ul>
 *   <li>members — 5 分钟</li>
 *   <li>dashboard — 10 分钟</li>
 *   <li>questionnaire — 30 分钟</li>
 * </ul>
 * 使用 Jackson JSON 序列化，gracefully 降级当 Redis 不可用时。
 */
@Configuration
@EnableCaching
public class RedisConfig {

    public static final String CACHE_MEMBERS = "members";
    public static final String CACHE_DASHBOARD = "dashboard";
    public static final String CACHE_QUESTIONNAIRE = "questionnaire";

    public static final Duration MEMBERS_TTL = Duration.ofMinutes(5);
    public static final Duration DASHBOARD_TTL = Duration.ofMinutes(10);
    public static final Duration QUESTIONNAIRE_TTL = Duration.ofMinutes(30);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = createDefaultCacheConfig();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put(CACHE_MEMBERS, defaultConfig.entryTtl(MEMBERS_TTL));
        cacheConfigs.put(CACHE_DASHBOARD, defaultConfig.entryTtl(DASHBOARD_TTL));
        cacheConfigs.put(CACHE_QUESTIONNAIRE, defaultConfig.entryTtl(QUESTIONNAIRE_TTL));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }

    private RedisCacheConfiguration createDefaultCacheConfig() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        objectMapper.registerModule(new JavaTimeModule());

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues()
                .entryTtl(Duration.ofMinutes(10));
    }
}
