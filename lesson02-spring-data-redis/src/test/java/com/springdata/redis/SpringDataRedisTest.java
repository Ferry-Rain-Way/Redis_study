package com.springdata.redis;
import com.alibaba.fastjson.JSON;
import com.springdata.redis.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.Map;

/**
 * @Version 1.0
 * @ClassName : SpringDataRedisTest
 * @Author : GUO_HONG_YU
 * @Description: 使用SpringDataRedis创建连接并测试
 */
@SpringBootTest
public class SpringDataRedisTest {
    /**注入对象*/
    @Autowired
    private  RedisTemplate<String,Object> redisTemplate;

    /**
     * 测试自定义RedisTemplate对value是string类型的操作
     */
    @Test
    public void testRedisTemplateValueIsString(){
        ValueOperations<String, Object> vOperate = redisTemplate.opsForValue();
        // 设置string数据
        vOperate.set("age",13);
        // 获取string数据
        Object name = vOperate.get("age");
        System.out.println("age = " + name);
    }

    /**
     * 测试自定义RedisTemplate对value是对象类型的操作
     * 运行结果:
     * {
     *   "@class": "com.springdata.redis.entity.User",
     *   "name": "zhang_san",
     *   "age": 22
     * }
     * 总结:由以上结果可以发现,对于插入的对象数据,在实现序列化的同时
     *      数据多了@class,有点是可以自动实现序列化与反序列化,
     *      在编写代码时很方便,缺点是占用内存空间
     */
    @Test
    public void testRedisTemplateValueIsObject(){
        ValueOperations<String, Object> vOperate  = redisTemplate.opsForValue();
        // 设置String数据(自动序列化)
        vOperate.set("user",new User("zhang_san",22));
        // 获取string数据(自动反序列化)
        Object obj = vOperate.get("user");
        System.out.println(obj);
    }

    /**
     * 测试Redis提供的StringRedisTemplate对value是对象类型的操作
     * 运行结果:
     * {
     *   "age": 23,
     *   "name": "lisi"
     * }
     * 总结:由以上结果可以发现,对于插入的对象数据
     * 序列化后的数据十分干净,节省内存空间
     * 但Redis提供的对象操作,需要手动实现对象的序列化与反序列化
     */
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Test
    public void testStringRedisTemplateValueIsObject(){
        ValueOperations<String, String> svOpera = stringRedisTemplate.opsForValue();
        /*  设置String数据
            手动序列化[p.s.]需要导入JSON序列化依赖,此处用的是阿里的fastjson)
         */
        String user = JSON.toJSONString(new User("lisi", 23));
        svOpera.set("user2",user);
        // 获取string数据(手动反序列化)
        String stringUser = svOpera.get("user2");
        User userObj = JSON.parseObject(stringUser, User.class);
        System.out.println(userObj);
    }

    @Test
    public void testStringRedisTemplateKeyIsHash(){
        HashOperations<String, Object, Object> hvOpera = stringRedisTemplate.opsForHash();
        // 设置Hash类型数据
            //单个存储
        hvOpera.put("blog:user","name","zhang_san");
        hvOpera.put("blog:user","age","34");
            //批量存储
        Map<String,String> map = new HashMap<>();
        map.put("address","北京市大兴区");
        map.put("email","123@qq.com");
        hvOpera.putAll("blog:user",map);

        // 获取hash类型数据[p.s. Entry输入项]
        Map<Object, Object> entries = hvOpera.entries("blog:user");
        System.out.println(entries);
    }
}
