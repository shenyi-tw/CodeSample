package com.example.demo.cache.impl;

import com.example.demo.cache.NotificationCache;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisNotificationCache implements NotificationCache {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String RECENT_KEY = "recent_notifications";
    private static final String LUA_PUSH_TRIM =
            "redis.call('LPUSH', KEYS[1], ARGV[1]); " +
                    "redis.call('LTRIM', KEYS[1], 0, 9); " +
                    "return 1;";

    @Override
    public void pushToRecentAtomic(Object value) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(LUA_PUSH_TRIM);
        script.setResultType(Long.class);

        // Execute script atomically
        redisTemplate.execute(script,
                Collections.singletonList(RECENT_KEY),
                value);
    }

    @Override
    public void put(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public void pushToRecent(Object value) {
        redisTemplate.opsForList().leftPush(RECENT_KEY, value);
        redisTemplate.opsForList().trim(RECENT_KEY, 0, 9);
    }

    @Override
    public List<Object> getRecent(int limit) {
        return redisTemplate.opsForList().range(RECENT_KEY, 0, limit - 1);
    }

    @Override
    public void clearRecent() {
        redisTemplate.delete(RECENT_KEY);
    }
}