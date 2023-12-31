package com.heima.item.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.heima.item.pojo.Item;
import com.heima.item.pojo.ItemStock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Version 1.0
 * @Date: 2023/12/12 15:51
 * @ClassName : CacheConfig
 * @Author : GUO_HONG_YU
 * @Description: JVM进程缓存
 */
@Configuration
public class CacheConfig {
    /**
     * 为商品添加缓存
     * @return 缓存
     */
    @Bean
    public Cache<Long, Item> itemCache(){
        return Caffeine.newBuilder()
                /* 初始容量100个 */
                .initialCapacity(100)
                /* 指定缓存可以包含的最大条目数
                *  当尺寸为零，则元素将在加载到缓存中后立即被逐出。这在测试中很有用，或者在不更改代码的情况下暂时禁用缓存
                *  */
                .maximumSize(10_000)
                .build();
    }

    /**
     * 为库存添加缓存
     * @return 缓存
     */
    @Bean
    public Cache<Long, ItemStock> stockCache(){
        return Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(10_000)
                .build();
    }
}
