package com.itsocoo.autocache.redis;

import com.itsocoo.autocache.redis.common.AspectTools;
import com.itsocoo.autocache.redis.properties.AspectSourceProperties;
import com.itsocoo.autocache.redis.redis.RedisManager;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationClassFilter;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * @author wanghaibo
 * @version V1.0
 * @desc 缓存的逻辑
 * @date 2019/1/21 10:52
 */
public class AspectAroundAdvisor implements PointcutAdvisor {
    private static final Logger logger = LoggerFactory.getLogger(AspectAroundAdvisor.class);

    // 缓存时间
    private static final long timeout = 3600L;
    private final RedisManager redisManager;
    private final CaffeineCacheManager caffeineCacheManager;
    private final AspectSourceProperties sourceProperties;
    private Cache CACHE;

    public AspectAroundAdvisor(RedisManager redisManager, CaffeineCacheManager caffeineCacheManager, AspectSourceProperties sourceProperties) {
        this.redisManager = redisManager;
        this.caffeineCacheManager = caffeineCacheManager;
        this.CACHE = caffeineCacheManager.getCache("redisCacheKeys");
        this.sourceProperties = sourceProperties;
    }

    @Override
    public Pointcut getPointcut() {
        /**
         * 简单的Pointcut定义，匹配所有类的find方法调用。
         */
        return new Pointcut() {

            @Override
            public ClassFilter getClassFilter() {
                return new AnnotationClassFilter(Service.class);
            }

            @Override
            public MethodMatcher getMethodMatcher() {
                return new MethodMatcher() {

                    @Override
                    public boolean matches(Method method, Class<?> targetClass) {
                        String methodName = method.getName();
                        if (targetClass.getSimpleName().contains(sourceProperties.getScanClassSuffix())) {
                            if (AspectTools.ifContainStr(methodName, sourceProperties.getPointcutQuery())) {
                                return true;
                            }
                        }
                        return false;
                    }

                    @Override
                    public boolean isRuntime() {
                        return false;
                    }

                    @Override
                    public boolean matches(Method method, Class<?> targetClass, Object[] args) {
                        return false;
                    }
                };
            }
        };
    }

    private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Override
    public Advice getAdvice() {

        return (MethodInterceptor) invocation -> {
            logger.debug("===============>调用RoundService之前成功");

            Method method = invocation.getMethod();

            try {
                // MethodSignature joinPointObject = (MethodSignature) proceedingJoinPoint.getSignature();
                // Method method = joinPointObject.getMethod();
                // 返回值类型
                Class<?> returnType = method.getReturnType();
                // 方法名
                String methodName = method.getName();
                // 获取参数名称
                String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
                // 获取参数类型
                Class[] parameterTypes = method.getParameterTypes();

                Class<?> aClass = invocation.getThis().getClass();

                // 获取包名
                String packageName = aClass.getName();
                // String simpleName = aClass.getSimpleName();
                // 参数(多个)
                Object[] args = invocation.getArguments();

                // TODO: 先获取缓存中的key没有就使用下面的方法
                String joiningKey = AspectTools.joiningKey(parameterNames);
                String cacheKey = AspectTools.joiningKey(packageName, methodName, joiningKey);

                String spELKey = CACHE.get(cacheKey, String.class);
                // Cache.ValueWrapper valueWrapper = CACHE.get(cacheKey,String.class);
                // String spELKey = (String) valueWrapper.get();

                String key;

                if (StringUtils.isNotBlank(spELKey)) {
                    logger.info("===============>查询：本地缓存spELKey存在:{}", spELKey);
                    // 直接解析spEL 然后进行缓存
                    String elKey = AspectUtil.parseSpELKeyToString(spELKey, method, args);
                    logger.info("===============>elKey:{}", elKey);
                    key = AspectTools.joiningKey(cacheKey, elKey);

                } else {
                    logger.info("===============>查询：缓存不存在！");
                    String spELStr;

                    if (methodName.contains(AspectTools.PARSE_SPLIT_PREFIX)) { // 有缓存标识 (findOne'CacheBy'IdAndBrand)
                        // 缓存不存在 则
                        List<Object> argsList = Arrays.asList(args);

                        logger.debug("===============>方法:{}", method);
                        logger.debug("===============>方法名:{}", methodName);
                        logger.debug("===============>包名:{}", packageName);
                        logger.debug("===============>返回值类型:{}", returnType);
                        logger.debug("===============>参数名称:{}", Arrays.asList(parameterNames));
                        logger.debug("===============>参数值:{}", argsList);

                        spELStr = AspectUtil.getPointArgsAction(methodName, parameterNames, parameterTypes, args);
                    } else {  // 没缓存标识 (list)
                        spELStr = AspectTools.getNoSplitPrefixString(parameterNames);
                    }

                    logger.info("===============>spELStr:{}", spELStr);

                    // 本地缓存 要删除的方法的spEL表达式
                    CACHE.put(cacheKey, spELStr);
                    // CACHE.put(cacheKey, AspectTools.joiningKey(packageName, methodName, joiningKey, spELStr));

                    // 将删除的方法的spEL表达式解析出值 拼接在一起作为redis删除缓存的key
                    key = AspectTools.joiningKey(packageName, methodName, joiningKey, AspectUtil.parseSpELKeyToString(spELStr, method, args));
                }

                logger.info("===============>缓存的键=key:{}", key);

                Object object = redisManager.get(key);

                // redis缓存也不存在
                if (object == null) {
                    object = invocation.proceed();
                    logger.info("===============>object={}", object);
                    // TODO: object=[]
                    // 判断结果有值的时候进行缓存
                    if (object != null) {
                        redisManager.set(key, object, timeout);
                    }
                }
                return object;

            } catch (Throwable throwable) {
                logger.error("\n===============>throwable:{}", throwable);
            }

            return null;
        };

    }

    @Override
    public boolean isPerInstance() {
        return true;
    }
}