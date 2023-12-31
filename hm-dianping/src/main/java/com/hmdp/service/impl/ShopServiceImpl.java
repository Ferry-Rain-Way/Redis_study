package com.hmdp.service.impl;


import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 尝试获取"锁":
     *  使用的原理是Redis中的SETNX命令
     *  若给定的 key已经存在,则SETNX不做任何动作
     * @param key 指定的key
     * @return 是否获取锁成功
     */
    private boolean tryLock(String key){
        // 注意此处的单位是秒
        Boolean aBoolean = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", 10L, TimeUnit.SECONDS);
        // 此处不要直接返回,因为可能会自动拆箱而返回null
        return Boolean.TRUE.equals(aBoolean);
    }

    /**
     * 删除"锁"
     * 删除指定的key,已达到释放锁的效果
     * @param key 指定的key
     * @return 删除锁是否成功
     */
    private boolean unlock(String key){
        Boolean delete = stringRedisTemplate.delete(key);
        return Boolean.TRUE.equals(delete);
    }

    /**
     * 根据id查询店铺
     * @param id 商铺id
     */
    @Override
    public Result queryById(Long id) {
//        Shop shop = queryShopWithMutex(id);
        Shop shop = queryShopWithLogicalExpire(id);
        return shop  == null
                ? Result.fail("店铺不存在")
                : Result.ok(shop);
    }

    public Shop queryShopWithLogicalExpire(Long id){
        // 判断缓存是否存在(数据理论上一定存在)
            // 数据存在但是为空、 数据不存在返回错误信息
        ValueOperations<String, String> sOpera = stringRedisTemplate.opsForValue();
        String key = CACHE_SHOP_KEY + id;
        String shopJson = sOpera.get(key);
        if(StrUtil.isBlank(shopJson)){
            return null;
        }
        // 将数据反序列化
        RedisData redisData = JSON.parseObject(shopJson, RedisData.class);
//        Shop shop = (Shop) redisData.getData();
        //2023-10-15 16:32:00.498 ERROR 156 --- [io-8081-exec-16]
        // com.hmdp.config.WebExceptionAdvice       :
        // java.lang.ClassCastException:
        //  class com.alibaba.fastjson.JSONObject cannot be cast to class com.hmdp.entity.Shop
        //  (com.alibaba.fastjson.JSONObject and com.hmdp.entity.Shop are in unnamed module of loader 'app')
        JSONObject jsonObj = (JSONObject) redisData.getData();
        Shop shop = jsonObj.toJavaObject(Shop.class);


        LocalDateTime expireTime = redisData.getExpireTime();
        // 判断数据是否过期
       if(expireTime.isAfter(LocalDateTime.now())){
            // 过期时间在当前时间之后
            // 没过期直接返回新数据
            return shop;
        }

        // 数据过期尝试获取锁
            // 获取所失败,直接返回旧数据
        boolean isSuccess = tryLock(LOCK_SHOP_KEY + id);

        // 获取锁成功
        if (isSuccess){
            // 第二次检查数据是否过期
            if(expireTime.isAfter(LocalDateTime.now())){
                // 过期时间在当前时间之后
                // 没过期直接返回新数据
                return shop;
            }else{
                // 数据过期
                // 返回旧数据 ,并开启新的线程
                CACHE_REBUILD_EXECUTOR.submit( () -> {
                    // 查询数据库
                    Shop newShopInfo = getById(id);

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
                    // 释放锁
                    unlock(LOCK_SHOP_KEY + id);
                });

            }
        }

        return shop;

    }



    /**
     * 通过互斥锁解决缓存击穿
     * @param id 店铺id
     * @return 店铺
     * Mutex : 互斥
     */
    public Shop queryShopWithMutex(Long id){
        // 注:此处更适合使用Hash类型,为了练习使用String类型
        ValueOperations<String, String> sOpera = stringRedisTemplate.opsForValue();
        String key = CACHE_SHOP_KEY + id;
        // 1.从Redis中查询商铺缓存
        String shopJson = sOpera.get(key);
        // 2.商户存在 → 返回商铺信息
        if (StrUtil.isNotBlank(shopJson)) {
            // 转成Shop类型:[fastjson]
            Shop shop = JSON.parseObject(shopJson, Shop.class);
            return shop;
        }
        // 3.判断是否为空值
        if("".equals(shopJson)) {
            return null;
        }

       String cacheLockKey = "cache:shop:lock:" + id;
       try{
           // 4.缓存数据不存在
           // 4.1获取互斥锁
           boolean isSuccess = tryLock(cacheLockKey);
           // 4.2获取互斥锁失败,休眠并重新尝试
           if(!isSuccess){
               Thread.sleep(50);
               queryShopWithMutex(id);

           }
           // 4.3获取成功
           // 4.3.1 做双重检查(DoubleCheck),检查缓存是否存在
           String checkShopInfo = sOpera.get(key);
           if (StrUtil.isBlank(checkShopInfo)) {
               // 4.3.2 缓存不存在,重建缓存
               // 缓存中商户不存在 → 查找数据库
               Shop byId = getById(id);
               // 存在 → 添加缓存并返回
               if (byId!=null){
                   String jsonString = JSON.toJSONString(byId);
                   sOpera.set(key,jsonString,CACHE_SHOP_TTL, TimeUnit.MINUTES);
                   return byId;
               }
               // 数据库中商户不存在
               // 向Redis中写入空值,规避缓存穿透
               sOpera.set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);

           }
           // 双重检查时发现数据存在,不重建缓存直接返回
           return  JSON.parseObject(checkShopInfo, Shop.class);

       } catch (InterruptedException e) {
           throw new RuntimeException(e);
       } finally{
           // 4.4 释放锁
           unlock(cacheLockKey);
       }
    }
//#region 缓存穿透
    /**
     * 解决不存在查询的缓存穿透问题
     * @param id 店铺id
     * @return 店铺
     */
    public Shop queryWithPassThrough(Long id){
        // 注:此处更适合使用Hash类型,为了练习使用String类型
        ValueOperations<String, String> sOpera = stringRedisTemplate.opsForValue();
        String key = CACHE_SHOP_KEY + id;
        // 从Redis中查询商铺缓存
        String shopJson = sOpera.get(key);
        // 商户存在 → 返回商铺信息
        if (StrUtil.isNotBlank(shopJson)) {
            // 转成Shop类型:[fastjson]
            Shop shop = JSON.parseObject(shopJson, Shop.class);
            return shop;
        }
        //判断是否为空值
        if("".equals(shopJson)) {
            return null;
        }
        // 缓存中商户不存在 → 查找数据库
        Shop byId = getById(id);
        // 存在 → 添加缓存并返回
        if (byId!=null){
            String jsonString = JSON.toJSONString(byId);
            sOpera.set(key,jsonString,CACHE_SHOP_TTL, TimeUnit.MINUTES);
            return byId;
        }
        // 数据库中商户不存在
        // 向Redis中写入空值,规避缓存穿透
        sOpera.set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
        return null;
    }
//#endregion
    /**
     * Redis缓存 - 更新数据库
     * @param shop 店铺
     */
    @Override
    @Transactional
    public Result update(Shop shop) {
        if (shop.getId() == null) {
            return Result.fail("商铺ID不能为空");
        }
        // 更新Mysql数据库
        updateById(shop);
        // 删除Redis缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}
