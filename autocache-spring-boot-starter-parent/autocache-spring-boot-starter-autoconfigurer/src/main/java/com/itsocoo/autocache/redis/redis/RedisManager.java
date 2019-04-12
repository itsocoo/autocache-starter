package com.itsocoo.autocache.redis.redis;

import com.itsocoo.autocache.redis.serializer.RedisObjectSerializer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author wanghaibo
 * @version V1.0
 * @desc RedisTemplate 缓存常用操作方法的封装
 * @date 2018/12/24 15:33
 */
public class RedisManager {
    private final RedisTemplate<String, Object> redisTemplate;

    private final RedisObjectSerializer valueSerializer;
    private final RedisSerializer<String> keySerializer;

    @Autowired
    public RedisManager(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        valueSerializer = (RedisObjectSerializer) redisTemplate.getValueSerializer();
        keySerializer = redisTemplate.getStringSerializer();
    }

    /**
     * 存储数据
     */
    public boolean set(String key, Object value) {
        return redisTemplate.execute((RedisCallback<Boolean>) connection -> {
            connection.set(keySerializer.serialize(key), valueSerializer.serialize(value));
            return true;
        });
    }

    /**
     * 存储数据
     */
    public boolean set(String key, Object value, long timeout) {
        return redisTemplate.execute((RedisCallback<Boolean>) connection -> {
            connection.setEx(keySerializer.serialize(key), timeout, valueSerializer.serialize(value));
            return true;
        });
    }

    /**
     * 获取数据
     */
    public Object get(String key) {
        return redisTemplate.execute((RedisCallback<Object>) connection -> {
            byte[] value = connection.get(keySerializer.serialize(key));
            return valueSerializer.deserialize(value);
        });
    }


    /**
     * 设置过期时间
     */
    public boolean expire(String key, final long expire) {
        return redisTemplate.expire(key, expire, TimeUnit.SECONDS);
    }

    /**
     * 删除
     */
    public Long remove(String key) {
        return redisTemplate.execute((RedisCallback<Long>) connection -> connection.del(keySerializer.serialize(key)));
    }


    /**
     * 通过前缀获取所有的缓存键：key*
     */
    public List<String> keys(String pattern) {
        if (StringUtils.isBlank(pattern)) {
            return new ArrayList<>();
        }

        byte[] rawKey = keySerializer.serialize(pattern);

        Set<byte[]> rawKeys = redisTemplate.execute(connection -> connection.keys(rawKey), true);

        return rawKeys.stream().map(keySerializer::deserialize).collect(Collectors.toList());
    }

    /**
     * 删除多个key
     */
    public Long delete(Collection<String> keys) {

        if (CollectionUtils.isEmpty(keys)) {
            return 0L;
        }

        final byte[][] rawKeys = new byte[keys.size()][];

        int i = 0;
        for (String key : keys) {
            rawKeys[i++] = keySerializer.serialize(key);
        }

        return redisTemplate.execute(connection -> connection.del(rawKeys), true);
    }
}