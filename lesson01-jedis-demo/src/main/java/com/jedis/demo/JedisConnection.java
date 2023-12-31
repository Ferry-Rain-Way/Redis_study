package com.jedis.demo;

import redis.clients.jedis.Jedis;

import java.util.Map;

/**
 * @Version 1.0
 * @ClassName : JedisConnection
 * @Author : GUO_HONG_YU
 * @Description: 简单的Jedis的连接与使用
 */
public class JedisConnection {
    private static final String COMPUTER_IP = "192.168.86.127";
    private static final Integer REDIS_PORT = 6379;
    private static final String REDIS_AUTH = "123456";
    private static final Integer REDIS_DATABASE = 0;

    public static void main(String[] args) {
        //1.创建连接
        /*创建Jedis对象*/
        Jedis jedis = new Jedis(COMPUTER_IP, REDIS_PORT);
        /*填写密码*/
        String auth = jedis.auth(REDIS_AUTH);
        /*选择数据库*/
        jedis.select(REDIS_DATABASE);

        //2.使用
        jedis.set("name","zhang_san");
        jedis.hset("user:1", "name", "Jack");
        String name = jedis.get("name");
        Map<String, String> map = jedis.hgetAll("user:1");

        System.out.println("String:"+name);
        System.out.println("HASH"+map);

        //3.资源释放
        jedis.close();
    }

}
