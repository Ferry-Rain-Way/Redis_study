package com.hmdp.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @Version 1.0
 * @ClassName : RedisWorkerID
 * @Author : GUO_HONG_YU
 * @Description: 生成全局唯一id
 *  符号位(1) + 时间戳(32) + 序列号(32)
 */
@Component
public class RedisWorkerID {

    /**
     *  开始时间
     *  LocalDateTime time = LocalDateTime.of(2023, 1, 1, 0, 0, 1);
     *  long epochSecond = time.toEpochSecond(ZoneOffset.UTC);// 1672531201
     */
    private static final long BEGIN_TIMESTAMP = 1672531201L;
    private static final int TIMESTAMP_LENGTH = 32;

    @Resource
    private   StringRedisTemplate stringRedisTemplate;

    public   long nextId(String keyPrefix){
        // 1.生成时间戳
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        long timestamp =  now.toEpochSecond(ZoneOffset.UTC) - BEGIN_TIMESTAMP;

        // 2.生成序列号
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 2.1自增长
        Long increment = stringRedisTemplate.opsForValue().increment("incr:" + keyPrefix + ":" + date);
        return (timestamp << TIMESTAMP_LENGTH | (increment==null?0:increment));
    }

}
