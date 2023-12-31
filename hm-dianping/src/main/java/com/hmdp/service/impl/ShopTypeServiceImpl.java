package com.hmdp.service.impl;

import com.alibaba.fastjson.JSON;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /*private RedisTemplate<String,ShopType> redisTemplate;*/
    @Override
    public Result getAll() {
        /*ListOperations<String, ShopType> listShopTypeOpera = redisTemplate.opsForList();*/
        ListOperations<String, String> listStringOpera = stringRedisTemplate.opsForList();

        //查询缓存中是否存在商户类型
        /*List<ShopType> shopTypes = listShopTypeOpera.range(CACHE_SHOP_TYPE_KEY, 0, -1L);*/
        List<String> range = listStringOpera.range(CACHE_SHOP_TYPE_KEY, 0, -1);
        // 手动反序列化
        List<ShopType> shopTypes = new ArrayList<>();
        if (range!=null&&!range.isEmpty()){
           for (int i=0;i<range.size();i++){
               String r = range.get(i);
               shopTypes.add(JSON.parseObject(r, ShopType.class));
           }
            //存在类型
            return Result.ok(shopTypes);
        }


        //查询数据库
        shopTypes = query().orderByAsc("sort").list();
        if (shopTypes == null || shopTypes.isEmpty()){
            return Result.fail("类型数据不存在");
        }
        // 手动序列化
        for(int i=0;i<shopTypes.size();i++){
            ShopType shopType = shopTypes.get(i);
            if(range==null){range = new ArrayList<>();}
            range.add(JSON.toJSONString(shopType));
        }
        //存储到Redis
        listStringOpera.rightPushAll(CACHE_SHOP_TYPE_KEY,range);
        return Result.ok(shopTypes);
    }
}
