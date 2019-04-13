package com.itsocoo.autocache.redis;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.itsocoo.autocache.redis.init.RoutingBeanPostProcessor;
import com.itsocoo.autocache.redis.properties.AspectSourceProperties;
import com.itsocoo.autocache.redis.redis.RedisManager;
import com.itsocoo.autocache.redis.serializer.RedisObjectSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author wanghaibo
 * @version V1.0
 * @Description 配置AOP
 * @date 2018/3/23 14:12
 */
@Configuration
@ConditionalOnProperty(name = "lzsz.cache.aspect.enable", havingValue = "true")
@EnableConfigurationProperties(AspectSourceProperties.class)
@Import({
        // MyBeanFactoryPostProcessor.class,
        // RedisCacheScannerRegistrar.class,
        RoutingBeanPostProcessor.class
})
public class AspectSourceAutoConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(AspectSourceAutoConfiguration.class);

    /**
     * 创建基于Caffeine的Cache Manager
     * ---------------------
     * Caffeine配置说明：
     * <p>
     * initialCapacity=[integer]: 初始的缓存空间大小
     * maximumSize=[long]: 缓存的最大条数
     * maximumWeight=[long]: 缓存的最大权重
     * expireAfterAccess=[duration]: 最后一次写入或访问后经过固定时间过期
     * expireAfterWrite=[duration]: 最后一次写入后经过固定时间过期
     * refreshAfterWrite=[duration]: 创建缓存或者最近一次更新缓存后经过固定的时间间隔，刷新缓存
     * weakKeys: 打开key的弱引用
     * weakValues：打开value的弱引用
     * softValues：打开value的软引用
     * recordStats：开发统计功能
     * ---------------------
     * 注意：
     * <p>
     * expireAfterWrite和expireAfterAccess同事存在时，以expireAfterWrite为准。
     * maximumSize和maximumWeight不可以同时使用
     * weakValues和softValues不可以同时使用
     * <p>
     * <p>
     * 注意：
     * expireAfterWrite和expireAfterAccess同时存在时，以expireAfterWrite为准。
     * maximumSize和maximumWeight不可以同时使用
     * <p>
     * expireAfterWrite是在指定项在一定时间内没有创建/覆盖时，会移除该key，下次取的时候从loading中取
     * expireAfterAccess是指定项在一定时间内没有读写，会移除该key，下次取的时候从loading中取
     * refreshAfterWrite是在指定时间内没有被创建/覆盖，则指定时间过后，再次访问时，会去刷新该缓存，在新值没有到来之前，始终返回旧值
     * 跟expire的区别是，指定时间过后，expire是remove该key，下次访问是同步去获取返回新值
     * 而refresh则是指定时间后，不会remove该key，下次访问会触发刷新，新值没有回来时返回旧值
     * ---------------------
     */
    @Bean
    @Primary
    public CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        Caffeine caffeine = Caffeine.newBuilder()
                //cache的初始容量值
                .initialCapacity(100)
                //maximumSize用来控制cache的最大缓存数量，maximumSize和maximumWeight不可以同时使用，
                .maximumSize(1000)
                //控制最大权重
                // .maximumWeight(100);
                // .expireAfter();
                // .expireAfterWrite()
                //使用refreshAfterWrite必须要设置cacheLoader
                .refreshAfterWrite(5, TimeUnit.SECONDS);
        cacheManager.setCaffeine(caffeine);
        cacheManager.setCacheLoader(cacheLoader());
        cacheManager.setCacheNames(Arrays.asList("redisCacheKeys", "otherCacheKeys"));
        // cacheManager.setAllowNullValues(false);
        return cacheManager;
    }

    /**
     * 必须要指定这个Bean，refreshAfterWrite=5s这个配置属性才生效
     */
    @Bean
    public CacheLoader<Object, Object> cacheLoader() {

        return new CacheLoader<Object, Object>() {

            @Override
            public Object load(Object key) throws Exception {
                return null;
            }

            // 重写这个方法将oldValue值返回回去，进而刷新缓存
            @Override
            public Object reload(Object key, Object oldValue) throws Exception {
                return oldValue;
            }
        };
    }

    @Bean("redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        //Long类型不可以会出现异常信息;
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        RedisObjectSerializer redisObjectSerializer = new RedisObjectSerializer();

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(redisObjectSerializer);
        return template;
    }

    @Bean
    public RedisManager redisManager(RedisTemplate<String, Object> redisTemplate) {
        return new RedisManager(redisTemplate);
    }

    @Bean
    @Primary
    public AspectAfterAdvisor createAspectAfterAdvisor(RedisManager redisManager, CaffeineCacheManager caffeineCacheManager, AspectSourceProperties sourceProperties) {
        return new AspectAfterAdvisor(redisManager, caffeineCacheManager, sourceProperties);
    }

    @Bean
    @Primary
    public AspectSourceProperties createAspectSourceProperties() {
        return new AspectSourceProperties();
    }

    @Bean
    @Primary
    public AspectAroundAdvisor createAspectAroundAdvisor(RedisManager redisManager, CaffeineCacheManager caffeineCacheManager, AspectSourceProperties sourceProperties) {
        return new AspectAroundAdvisor(redisManager, caffeineCacheManager, sourceProperties);
    }
}