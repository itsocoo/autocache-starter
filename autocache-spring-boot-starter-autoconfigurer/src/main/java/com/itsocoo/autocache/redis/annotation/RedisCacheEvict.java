package com.itsocoo.autocache.redis.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * @author wanghaibo
 * @version V1.0
 * @desc 要删除的缓存标识
 * @date 2018/12/24 13:52
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface RedisCacheEvict {

    // 要清除的缓存的方法
    @AliasFor("cacheMethodNames")
    String[] value() default {};

    // 参数 SpEL
    @AliasFor("value")
    String[] cacheMethodNames() default {};
}
