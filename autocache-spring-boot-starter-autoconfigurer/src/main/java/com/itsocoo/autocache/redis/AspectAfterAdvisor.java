package com.itsocoo.autocache.redis;

import com.itsocoo.autocache.redis.annotation.RedisCacheEvict;
import com.itsocoo.autocache.redis.common.AspectTools;
import com.itsocoo.autocache.redis.common.CheckDeclaredMethods;
import com.itsocoo.autocache.redis.properties.AspectSourceProperties;
import com.itsocoo.autocache.redis.redis.RedisManager;
import org.aopalliance.aop.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.*;
import org.springframework.aop.support.annotation.AnnotationClassFilter;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author wanghaibo
 * @version V1.0
 * @desc 删除缓存
 * @date 2019/1/21 10:51
 */
public class AspectAfterAdvisor implements PointcutAdvisor {
    private static final Logger logger = LoggerFactory.getLogger(AspectAfterAdvisor.class);

    // 缓存时间
    private static final long timeout = 3600L;
    private final RedisManager redisManager;
    private final CaffeineCacheManager caffeineCacheManager;
    private final AspectSourceProperties sourceProperties;
    private Cache CACHE;

    public AspectAfterAdvisor(RedisManager redisManager, CaffeineCacheManager caffeineCacheManager, AspectSourceProperties sourceProperties) {
        this.redisManager = redisManager;
        this.caffeineCacheManager = caffeineCacheManager;
        this.sourceProperties = sourceProperties;
        this.CACHE = caffeineCacheManager.getCache("redisCacheKeys");
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
                // return ClassFilter.TRUE;
            }

            @Override
            public MethodMatcher getMethodMatcher() {
                return new MethodMatcher() {

                    @Override
                    public boolean matches(Method method, Class<?> targetClass) {
                        String methodName = method.getName();
                        if (targetClass.getSimpleName().contains(sourceProperties.getScanClassSuffix())) {
                            // if ("find".equals(methodName)) {
                            if (AspectTools.ifContainStr(methodName, sourceProperties.getPointcutEvict())) {
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

        return (AfterReturningAdvice) (returnValue, method, args, target) -> {
            logger.debug("BeforeAdvice实现，在目标方法被调用前调用，目标方法是：" + method.getDeclaringClass().getName() + "."
                    + method.getName());

            // jdk1.8中直接通过joinPoint中的getSignature()方法即可获取参数名称
            // MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            // 方法
            // Method method = methodSignature.getMethod();
            // 方法名
            String methodName = method.getName();
            // 参数名
            String[] pointParameterNames = parameterNameDiscoverer.getParameterNames(method);
            logger.debug("===============>pointParameterNames:{}", pointParameterNames);
            // 参数类型
            Class[] pointParameterTypes = method.getParameterTypes();
            logger.debug("===============>pointParameterTypes:{}", pointParameterTypes);
            // 参数值
            Object[] pointParameterValues = args;
            logger.debug("===============>pointParameterValues:{}", pointParameterValues);
            // 获取目标类
            Class<?> aClass = target.getClass();
            // 获取目标类包名
            String packageName = aClass.getName();

            // 拼接本地缓存的key
            String evictCacheKey = AspectTools.joiningKey(packageName, methodName, AspectTools.joiningKey(pointParameterNames));

            // 获取要删除的标识
            RedisCacheEvict cacheEvict = method.getAnnotation(RedisCacheEvict.class);

            if (cacheEvict != null) {
                // 要删除的方法
                String[] methods = cacheEvict.value();

                // 解析spEL表达式
                if (!AspectTools.isArrayBoolean(methods)) {
                    // 不处理
                    return;
                }

                // 缓存的key
                List<String> keys = new ArrayList<>();

                // 通过上面的cacheEvict获取本地缓存的值中所有要删除的方法的keys
                List<String> spELKeys = CACHE.get(evictCacheKey, List.class);

                // Cache.ValueWrapper valueWrapper = CACHE.get(evictCacheKey);

                // List<String> spELKeys = (List<String>) valueWrapper.get();

                if (spELKeys != null && !spELKeys.isEmpty()) {
                    // 直接解析spEL 然后进行缓存
                    List<String> stringList = spELKeys.stream().map(spELKey -> AspectUtil.parseSpELKey2String(spELKey, method, pointParameterValues)).collect(Collectors.toList());
                    logger.debug("===============>stringList:{}", stringList);

                    if (!stringList.isEmpty()) {
                        logger.info("===============>删除：本地缓存stringList存在:{}", stringList);

                        // 上面的结果中找出模糊查询的key 然后使用redis查询出所有的匹配模糊的key 合并在一起
                        Map<Boolean, List<String>> booleanListMap = stringList.stream().collect(Collectors.partitioningBy(o -> o.endsWith(AspectTools.ARGS_JOINT_MARK_FUZZY)));
                        List<String> fuzzyFalseList = booleanListMap.get(false);
                        // 模糊的key
                        List<String> fuzzyTrueList = booleanListMap.get(true);

                        // redis查询所有模糊查询的key
                        List<String> tmpTrueList = fuzzyTrueList.stream().flatMap(s -> redisManager.keys(s).stream()).collect(Collectors.toList());
                        // 合并在一起 缓存spEL
                        fuzzyFalseList.addAll(tmpTrueList);
                        // 一次调用redis删除
                        Long delete = redisManager.delete(fuzzyFalseList);
                        logger.info("===============>delete:{}", delete);
                        return;
                    }
                }
                // TODO: 下面是本地缓存不存在时候
                logger.info("===============>删除：本地缓存stringList不存在！");
                LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();
                // 要验证方法名称是否真实存在 去掉不存在的方法
                CheckDeclaredMethods checkDeclaredMethods = CheckDeclaredMethods.newBuilder().withAClass(aClass).withMethods(methods).withU(u).build();

                methods = checkDeclaredMethods.getMethods();
                Map<String, String[]> stringMap = checkDeclaredMethods.getStringMap();

                // 解析出方法中的几种状态
                Map<String, Set<Map<String, String>>> actionWay = AspectUtil.boostParseMethods(methods);

                if (actionWay.isEmpty()) return;

                logger.info("===============>actionWay:{}", actionWay);

                Set<Map<String, String>> fuzzySet = actionWay.get("fuzzy");
                Set<Map<String, String>> autoSet = actionWay.get("auto");
                Set<Map<String, String>> accuratelySet = actionWay.get("accurately");
                Set<Map<String, String>> fuzzyExtSet = actionWay.get("fuzzyExt");

                logger.debug("===============>fuzzySet:{}", fuzzySet);
                logger.debug("===============>autoSet:{}", autoSet);
                logger.debug("===============>accuratelySet:{}", accuratelySet);
                logger.debug("===============>fuzzyExtSet:{}", fuzzyExtSet);

                //TODO =======================下面全部先解析为spEL 为了能够缓存的目的================================
                List<String> spELCacheLists = new ArrayList<>();

                // 1.解析自定义的spEL的键
                if (accuratelySet != null && !accuratelySet.isEmpty()) {
                    List<String> collect = accuratelySet.stream().map(stringStringMap -> {
                        // 拼接缓存的键
                        String tmpMethodName = stringStringMap.get("m");
                        String spELStr = stringStringMap.get("a");

                        // 本地缓存 要删除的方法的spEL表达式
                        spELCacheLists.add(AspectTools.joiningKey(packageName, tmpMethodName, AspectTools.joiningKey(stringMap.get(tmpMethodName)), spELStr));

                        // 将删除的方法的spEL表达式解析出值 拼接在一起作为redis删除缓存的key
                        return AspectTools.joiningKey(packageName, tmpMethodName, AspectTools.joiningKey(stringMap.get(tmpMethodName)), AspectUtil.parseSpELKeyToString(spELStr, method, pointParameterValues));

                    }).filter(s -> s.length() > 0).collect(Collectors.toList());

                    keys.addAll(collect);
                }

                // 2.解析自动处理的键
                if (autoSet != null && !autoSet.isEmpty()) {
                    // TODO: 从@cacheEvict方法填写的values中解析出每个方法的参数的名称（约定俗成）xxByOrderIdAndName……
                    // 将所有解析的参数拼接为spEl表达式
                    List<String> collect = autoSet.stream().map(stringStringMap -> {
                        String tmpMethodName = stringStringMap.get("m");
                        String spELStr;

                        // 要处理没有By缓存标识的方法名称 (list)
                        if (!tmpMethodName.contains(AspectTools.PARSE_SPLIT_PREFIX)) { // 有缓存标识 (findOne'CacheBy'IdAndBrand)
                            // 没缓存标识 (list) 添加上缓存标识
                            // spELArg = AspectTools.getNoSplitPrefixString(parameterNames);
                            // 获取这个方法本身的参数组合成缓存标识
                            String[] strings = stringMap.get(tmpMethodName);
                            String tmp = AspectTools.PARSE_SPLIT_PREFIX + AspectTools.joiningConnect(strings);
                            spELStr = AspectUtil.getPointArgsAction(tmp, pointParameterNames, pointParameterTypes, pointParameterValues);
                        } else {
                            spELStr = AspectUtil.getPointArgsAction(tmpMethodName, pointParameterNames, pointParameterTypes, pointParameterValues);
                        }

                        // 本地缓存 要删除的方法的spEL表达式
                        spELCacheLists.add(AspectTools.joiningKey(packageName, tmpMethodName, AspectTools.joiningKey(stringMap.get(tmpMethodName)), spELStr));

                        // 将删除的方法的spEL表达式解析出值 拼接在一起作为redis删除缓存的key
                        return AspectTools.joiningKey(packageName, tmpMethodName, AspectTools.joiningKey(stringMap.get(tmpMethodName)), AspectUtil.parseSpELKeyToString(spELStr, method, pointParameterValues));

                    }).filter(s -> s.length() > 0).collect(Collectors.toList());

                    keys.addAll(collect);
                }

                // 3.解析模糊匹配前缀的键
                if (fuzzySet != null && !fuzzySet.isEmpty()) {
                    // 模糊 匹配前缀 后查出所有的key 依次删除
                    List<String> collect = fuzzySet.stream().map(stringStringMap -> stringStringMap.get("m")).flatMap(m -> redisManager.keys(AspectTools.keyGeneratorFuzzy(packageName, m, AspectTools.joiningKey(stringMap.get(m)))).stream()).filter(s -> s.length() > 0).collect(Collectors.toList());

                    // 将模糊的key直接缓存在本地中
                    // List<String> list = fuzzySet.stream().map(stringStringMap -> stringStringMap.get("m")).collect(Collectors.toList());

                    List<String> list = fuzzySet.stream().map(stringStringMap -> {
                        String tmpMethodName = stringStringMap.get("m");

                        return AspectTools.joiningKey(packageName, tmpMethodName, AspectTools.joiningKey(stringMap.get(tmpMethodName))) + AspectTools.ARGS_JOINT_MARK_FUZZY;
                    }).filter(s -> s.length() > 0).collect(Collectors.toList());

                    // 本地缓存spEL
                    spELCacheLists.addAll(list);

                    keys.addAll(collect);
                }

                // 4.解析扩展的模糊匹配前缀的键 (#id*)
                if (fuzzyExtSet != null && !fuzzyExtSet.isEmpty()) {

                    List<String> collect = fuzzyExtSet.stream().map(stringStringMap -> {
                        // 拼接缓存的键
                        String tmpMethodName = stringStringMap.get("m");
                        String s = stringStringMap.get("a");
                        String spELStr = s.substring(0, s.length() - AspectTools.ARGS_JOINT_MARK_FUZZY.length());

                        // 将删除的方法的spEL表达式解析出值 拼接在一起作为redis删除缓存的key
                        // 模糊 匹配前缀 后查出所有的key 依次删除
                        return AspectTools.joiningKey(packageName, tmpMethodName, AspectTools.joiningKey(stringMap.get(tmpMethodName)), AspectUtil.parseSpELKeyToString(spELStr, method, pointParameterValues)) + AspectTools.ARGS_JOINT_MARK_FUZZY;
                    }).filter(s -> s.length() > 0).flatMap(m -> redisManager.keys(m).stream()).collect(Collectors.toList());

                    List<String> list = fuzzyExtSet.stream().map(stringStringMap -> {
                        String tmpMethodName = stringStringMap.get("m");

                        String s = stringStringMap.get("a");

                        return AspectTools.joiningKey(packageName, tmpMethodName, AspectTools.joiningKey(stringMap.get(tmpMethodName))) + s;
                    }).filter(s -> s.length() > 0).collect(Collectors.toList());

                    // 缓存spEL
                    spELCacheLists.addAll(list);

                    keys.addAll(collect);
                }

                logger.info("===============>keys:{}", keys);

                // 依次删除
                if (!keys.isEmpty()) {
                    Long delete = redisManager.delete(keys);
                    logger.info("===============>delete:{}", delete);
                }

                // 缓存上面的值
                CACHE.put(evictCacheKey, spELCacheLists);
            }
        };
    }

    @Override
    public boolean isPerInstance() {
        return true;
    }
}