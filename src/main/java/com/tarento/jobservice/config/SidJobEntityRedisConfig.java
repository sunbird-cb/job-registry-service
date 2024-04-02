package com.tarento.jobservice.config;

import com.tarento.jobservice.entity.JobEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class SidJobEntityRedisConfig {
  @Bean
  public RedisTemplate<String, JobEntity> redisTemplateForSidJobEntity(
      RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, JobEntity> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(connectionFactory);
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    return redisTemplate;
  }
}
