package com.liud.ainocodegenerator.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Redis 配置类
 */
@Configuration
@EnableRedisHttpSession(
        maxInactiveIntervalInSeconds = 1800,  // 30分钟
        redisNamespace = "ai:nocode:session"   // 统一命名空间
)
public class RedisConfig {

    /**
     * 配置 Redis Session 序列化器，支持 Java 8 日期时间类型
     */
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 注册 Java 8 日期时间模块
        objectMapper.registerModule(new JavaTimeModule());
        // 禁用将日期写为时间戳格式，使用 ISO-8601 格式
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 启用默认类型信息，以便反序列化时知道具体的类
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
