package com.hmdp.config;

import com.hmdp.entity.ShopType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * @Version 1.0
 * @ClassName : RedisTemplateSerializerConfig
 * @Author : GUO_HONG_YU
 * @Description: 自定义配置RedisTemplate的序列化
 * 采用了JSON序列化来代替默认的JDK序列化方式
 */
@Configuration
public class RedisTemplateSerializerConfig {

    /**
     *  设置Key和Hash的key的序列化方式(String类型)
     *  设置Key和Hash的value的序列化方式(Json类型)
     * @param redisConnectionFactory Redis连接工厂
     * @return 对Redis操作的RedisTemplate对象
     */
    @Bean
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
        //创建RedisTemplate对象
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        //设置连接工厂
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //设置Key和Hash的key的序列化方式(String类型)
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setHashKeySerializer(RedisSerializer.string());
        //设置Key和Hash的value的序列化方式(Json类型)
        GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer
                = new GenericJackson2JsonRedisSerializer();
        redisTemplate.setHashValueSerializer(genericJackson2JsonRedisSerializer);
        redisTemplate.setValueSerializer(genericJackson2JsonRedisSerializer);

        return redisTemplate;
    }
}

