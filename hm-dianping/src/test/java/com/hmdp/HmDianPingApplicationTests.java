package com.hmdp;

import com.alibaba.fastjson.JSON;
import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.RedisData;
import com.hmdp.utils.RedisWorkerID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;


@SpringBootTest
class HmDianPingApplicationTests {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ShopServiceImpl service;

    // 数据预热
    @Test
    public void testInsertValueToRedis(){
        ValueOperations<String, String> sOpera = stringRedisTemplate.opsForValue();
        String key = CACHE_SHOP_KEY + 1;

        // 查询数据库
        Shop newShopInfo = service.getById(1);

        if(newShopInfo == null){
            // 数据库数据不存在 → 缓存空值
            sOpera.set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
        }
        // 数据库数据存在 → 重建Redis缓存 → 更新逻辑过期时间
        try {
            // 让缓存重建的过程慢一点,看到数据不一致的现象
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // 序列化数据
        RedisData newData = new RedisData();
        newData.setData(newShopInfo);
        // 设置逻辑上的过期时间
        newData.setExpireTime(LocalDateTime.now().plusSeconds(CACHE_SHOP_TTL));
        String jsonString = JSON.toJSONString(newData);
        // 重建缓存
        sOpera.set(key,jsonString);

    }


    @Resource
    RedisWorkerID redisWorkerID;
    @Test
    public void testInject(){
        long shop = redisWorkerID.nextId("shop");
        System.out.println(shop);
        System.out.println(LocalDateTime.now());
    }
}
