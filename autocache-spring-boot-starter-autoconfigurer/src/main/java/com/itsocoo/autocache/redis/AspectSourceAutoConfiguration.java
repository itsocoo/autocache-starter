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