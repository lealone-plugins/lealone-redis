/*
 * Copyright Lealone Database Group.
 * Licensed under the Server Side Public License, v 1.
 * Initial Developer: zhh
 */
package com.lealone.plugins.redis.test;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import com.lealone.test.TestBase;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPooled;

public class RedisTest extends TestBase {
    @Test
    public void run() {
        String host = "127.0.0.1";
        int port = 6379;
        port = 9610;

        JedisPool pool = new JedisPool(host, port);
        try (Jedis jedis = pool.getResource()) {
            String ret = jedis.set("foo", "bar");
            System.out.println("ret: " + ret);
            System.out.println(jedis.get("foo"));

            Map<String, String> hash = new HashMap<>();
            hash.put("name", "zhh");
            hash.put("age", "18");
            // jedis.hset("user-session:123", hash);
            // System.out.println(jedis.hgetAll("user-session:123"));
        }
        pool.close();

        JedisPooled jedis = new JedisPooled(host, port);
        String ret = jedis.set("foo", "bar");
        System.out.println("ret: " + ret);
        System.out.println(jedis.get("foo"));
        jedis.close();
    }
}
