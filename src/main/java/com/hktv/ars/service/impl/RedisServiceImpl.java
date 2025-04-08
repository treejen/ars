package com.hktv.ars.service.impl;

import com.hktv.ars.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void saveToRedis(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    @Override
    public String getFromRedis(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }
}
