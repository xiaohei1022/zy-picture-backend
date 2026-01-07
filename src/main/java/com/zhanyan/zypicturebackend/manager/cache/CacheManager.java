package com.zhanyan.zypicturebackend.manager.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class CacheManager {

    private Cache<String, Object> localCache;

    @Bean
    public Cache<String, Object> localCache() {
        return localCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 辅助方法：构造复合 key
    private String buildCacheKey(String hashKey, String key) {
        return hashKey + ":" + key;
    }

    /**
     * 获取缓存数据
     * @param compositeKey
     * @return
     */
    public Object get(String compositeKey) {
        // 1. 先查本地缓存
        Object value = localCache.getIfPresent(compositeKey);
        if (value != null) {
            log.info("本地缓存获取到数据 {} = {}", compositeKey, value);
            return value;
        }

        // 2. 本地缓存未命中，查询 Redis
        Object redisValue = stringRedisTemplate.opsForValue().get(compositeKey);
        if (redisValue != null) {
            // 更新本地缓存
            localCache.put(compositeKey, redisValue);
        }
        return redisValue;
    }

    /**
     * 设置缓存数据
     * @param compositeKe
     * @param value
     * @param cacheExpireTime
     */
    public void set(String compositeKe, String value, long cacheExpireTime) {
        // 1. 设置本地缓存
        localCache.put(compositeKe, value);
        // 2. 设置redis 缓存
        stringRedisTemplate.opsForValue().set(compositeKe, value, cacheExpireTime, TimeUnit.SECONDS);
    }
}
