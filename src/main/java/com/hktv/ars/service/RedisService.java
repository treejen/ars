package com.hktv.ars.service;

public interface RedisService {

    void saveToRedis(String key, String value);

    String getFromRedis(String key);
}
