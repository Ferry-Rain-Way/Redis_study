package com.heima.item.canal;

import com.github.benmanes.caffeine.cache.Cache;
import com.heima.item.config.RedisHandler;
import com.heima.item.pojo.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.javatool.canal.client.annotation.CanalTable;
import top.javatool.canal.client.handler.EntryHandler;

/**
 * @Version 1.0
 * @Date: 2023/12/13 11:25
 * @ClassName : ItemHandler
 * @Author : GUO_HONG_YU
 * @Description: Canal监听到商品数据发生变化执行操作
 *
 * `@CanalTable("tb_item")告诉当前关联的表
 *  且由于Canal底层并没有使用Mybatis,所以需要为对应的实体类添加注解
 */
@Component
@CanalTable("tb_item")
public class ItemHandler implements EntryHandler<Item> {

    @Autowired
    private RedisHandler redisHandler;
    @Autowired
    private Cache<Long, Item> itemCache;

    /**
     * 当有新的数据产生时执行
     * @param item 商品
     */
    @Override
    public void insert(Item item) {
        // 写数据到JVM进程缓存
        itemCache.put(item.getId(), item);
        // 写数据到redis
        redisHandler.saveItem(item);
    }

    /**
     * 当数据发生变动时执行
     * @param before 旧数据
     * @param after 新数据
     */
    @Override
    public void update(Item before, Item after) {
        // 写数据到JVM进程缓存
        itemCache.put(after.getId(), after);
        // 写数据到redis
        redisHandler.saveItem(after);
    }

    /**
     * 当有数据删除时执行
     * @param item 商品
     */
    @Override
    public void delete(Item item) {
        // 删除数据到JVM进程缓存
        itemCache.invalidate(item.getId());
        // 删除数据到redis
        redisHandler.deleteItemById(item.getId());
    }
}
