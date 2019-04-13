package com.itsocoo.autocache.redis.init;

import com.itsocoo.autocache.redis.annotation.RedisCacheEvict;
import com.itsocoo.autocache.redis.common.AspectTools;
import com.itsocoo.autocache.redis.common.CheckDeclaredMethods;
import com.itsocoo.autocache.redis.properties.AspectSourceProperties;
import com.itsocoo.autocache.redis.AspectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class RoutingBeanPostProcessor implements BeanPostProcessor {
    private static final Logger logger = LoggerFactory.getLogger(RoutingBeanPostProcessor.class);

    private final ApplicationContext applicationContext;

    @Autowired
    public RoutingBeanPostProcessor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        // 获取CaffeineCache
        CaffeineCacheManager caffeineCacheManager = applicationContext.getBean(CaffeineCacheManager.class);
        AspectSourceProperties sourceProperties = applicationContext.getBean(AspectSourceProperties.class);
        Cache CACHE = caffeineCacheManager.getCache("redisCacheKeys");

        // 获取扫描的包路径
        String[] basePackages = sourceProperties.getScanPackagePaths();
        if (basePackages == null) {
            return bean;
        }
        String pointcutEvict = sourceProperties.getPointcutEvict();
        String pointcutQuery = sourceProperties.getPointcutQuery();
        String scanClassSuffix = sourceProperties.getScanClassSuffix();
        // 获取所有的bean
        Class<?> beanClass = bean.getClass();

        // String simpleName = beanClass.getSimpleName();
        String simpleClassName = AspectTools.toUpperCaseFirstOne(beanName);
        // TODO: 去掉class com.sun.proxy.$Proxy113 会为空
        Package beanClassPackage = beanClass.getPackage();
        if (beanClassPackage == null) {
            return bean;
        }
        String beanPackageName = beanClassPackage.getName();
        String beanClassName = AspectTools.joiningKey(beanPackageName, simpleClassName);

        try {
            for (String basePackage : basePackages) {
                if (basePackage.equalsIgnoreCase(beanPackageName) && beanName.contains(scanClassSuffix)) {
                    logger.debug("===============>beanName:{}", beanName);
                    logger.debug("===============>beanPackageName:{}", beanPackageName);
                    logger.debug("===============>beanClassName:{}", beanClassName);

                    LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();

                    // Class<?> aClass;
                    // 获取被扫描到的类
                    Class<?> aClass = Class.forName(beanClassName);
                    // 获取目标类的方法
                    Method[] declaredMethods = aClass.getDeclaredMethods();

                    for (Method method : declaredMethods) {
                        boolean aStatic = AspectTools.isStaticMethod(method);
                        // 不处理静态方法
                        if (aStatic) {
                            continue;
                        }
                        // TODO: 解析所有方法为redis的key的spEl表达式 然后通过“包+方法名称”缓存
                        // TODO: 解析目标类的方法名
                        String methodName = method.getName();
                        // 获取目标类的方法的参数类型
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        // jdk8获取到目标类的方法的参数名称
                        // Parameter[] parameters = classMethod.getParameters();
                        // TODO 要使用jdk8的Method.getParameters(); 则必须在启动的时候设置jvm javac -parameters 否则获取的参数还是arg0这样的
                        String[] parameterNames = u.getParameterNames(method);
                        logger.debug("===============>parameterNames:{}", Arrays.asList(parameterNames));

                        // TODO: 缓存的键
                        String evictCacheKey = AspectTools.joiningKey(beanClassName, methodName, AspectTools.joiningKey(parameterNames));
                        logger.info("===============>缓存的键 evictCacheKey:{}", evictCacheKey);

                        if (AspectTools.ifContainStr(methodName, pointcutQuery)) {
                            logger.info("===============>methodName:{}", methodName);

                            if (methodName.contains(AspectTools.PARSE_SPLIT_PREFIX)) { // 有缓存标识 (findOne'CacheBy'IdAndBrand)
                                String toSpEL = AspectUtil.getPointArgsAction(methodName, parameterNames, parameterTypes, null);
                                logger.info("===============>toSpEL:{}", toSpEL);

                                // TODO 将结果缓存在内存中 存入一条字符串
                                CACHE.put(evictCacheKey, toSpEL);
                            } else {  // 没缓存标识 (list)
                                String toSpEL = AspectTools.getNoSplitPrefixString(parameterNames);

                                logger.info("===============>toSpEL:{}", toSpEL);
                                CACHE.put(evictCacheKey, toSpEL);
                            }
                        } else if (AspectTools.ifContainStr(methodName, pointcutEvict)) {// 修改
                            logger.info("===============>methodName:{}", methodName);
                            // 获取RedisCacheEvict注解
                            RedisCacheEvict redisCacheEvict = method.getAnnotation(RedisCacheEvict.class);
                            if (redisCacheEvict != null) {
                                // 要删除的方法
                                String[] methods = redisCacheEvict.value();

                                // 解析spEL表达式
                                if (!AspectTools.isArrayBoolean(methods)) {
                                    // 不处理
                                    return bean;
                                }

                                // 要验证方法名称是否真实存在 去掉不存在的方法
                                CheckDeclaredMethods checkDeclaredMethods = CheckDeclaredMethods.newBuilder().withAClass(aClass).withMethods(methods).withU(u).build();
                                methods = checkDeclaredMethods.getMethods();
                                Map<String, String[]> stringMap = checkDeclaredMethods.getStringMap();

                                // 解析出方法中的几种状态
                                Map<String, Set<Map<String, String>>> actionWay = AspectUtil.boostParseMethods(methods);

                                if (actionWay.isEmpty()) return bean;

                                logger.debug("===============>actionWay:{}", actionWay);

                                Set<Map<String, String>> fuzzySet = actionWay.get("fuzzy");
                                Set<Map<String, String>> autoSet = actionWay.get("auto");
                                Set<Map<String, String>> accuratelySet = actionWay.get("accurately");
                                Set<Map<String, String>> fuzzyExtSet = actionWay.get("fuzzyExt");

                                logger.debug("===============>fuzzySet:{}", fuzzySet);
                                logger.debug("===============>autoSet:{}", autoSet);
                                logger.debug("===============>accuratelySet:{}", accuratelySet);
                                logger.debug("===============>fuzzyExtSet:{}", fuzzyExtSet);

                                //TODO ====下面全部先解析为spEL 为了能够缓存的目的====
                                List<String> spELCacheLists = new ArrayList<>();

                                // 1.解析自定义的spEL的键
                                if (accuratelySet != null && !accuratelySet.isEmpty()) {

                                    List<String> collect = accuratelySet.stream().map(stringStringMap -> {
                                        // 拼接缓存的键
                                        String tmpMethodName = stringStringMap.get("m");
                                        String spELArg = stringStringMap.get("a");

                                        // 缓存spEL
                                        return AspectTools.joiningKey(basePackage, simpleClassName, tmpMethodName, AspectTools.joiningKey(stringMap.get(tmpMethodName)), spELArg);
                                    }).filter(s -> s.length() > 0).collect(Collectors.toList());

                                    spELCacheLists.addAll(collect);
                                }

                                // 2.解析自动处理的键
                                if (autoSet != null && !autoSet.isEmpty()) {

                                    // TODO: 从@cacheEvict方法填写的values中解析出每个方法的参数的名称（约定俗成）xxxCacheByOrderIdAndName……
                                    // 将所有解析的参数拼接为spEl表达式
                                    List<String> collect = autoSet.stream().map(stringStringMap -> {
                                        String tmpMethodName = stringStringMap.get("m");

                                        // 要处理没有By缓存标识的方法名称 (list)
                                        String spELArg;
                                        if (!tmpMethodName.contains(AspectTools.PARSE_SPLIT_PREFIX)) { // 有缓存标识 (findOne'CacheBy'IdAndBrand)
                                            // 没缓存标识 (list) 添加上缓存标识
                                            // spELArg = AspectTools.getNoSplitPrefixString(parameterNames);
                                            // 获取这个方法本身的参数组合成缓存标识
                                            String[] strings = stringMap.get(tmpMethodName);
                                            String tmp = AspectTools.PARSE_SPLIT_PREFIX + AspectTools.joiningConnect(strings);
                                            spELArg = AspectUtil.getPointArgsAction(tmp, parameterNames, parameterTypes, null);
                                        } else {
                                            spELArg = AspectUtil.getPointArgsAction(tmpMethodName, parameterNames, parameterTypes, null);
                                        }

                                        // 缓存spEL
                                        return AspectTools.joiningKey(basePackage, simpleClassName, tmpMethodName, AspectTools.joiningKey(stringMap.get(tmpMethodName)), spELArg);
                                    }).filter(s -> s.length() > 0).collect(Collectors.toList());

                                    spELCacheLists.addAll(collect);
                                }

                                // 3.解析模糊匹配前缀的键
                                if (fuzzySet != null && !fuzzySet.isEmpty()) {
                                    // 模糊 匹配前缀 后查出所有的key 依次删除
                                    List<String> collect = fuzzySet.stream().map(stringStringMap -> {
                                        String tmpMethodName = stringStringMap.get("m");

                                        return AspectTools.joiningKey(basePackage, simpleClassName, tmpMethodName, AspectTools.joiningKey(stringMap.get(tmpMethodName))) + AspectTools.ARGS_JOINT_MARK_FUZZY;
                                    }).filter(s -> s.length() > 0).collect(Collectors.toList());

                                    // 缓存spEL
                                    spELCacheLists.addAll(collect);
                                }

                                // 4.解析扩展的模糊匹配前缀的键 (#id*)
                                if (fuzzyExtSet != null && !fuzzyExtSet.isEmpty()) {

                                    // 模糊 匹配前缀 后查出所有的key 依次删除
                                    List<String> collect = fuzzyExtSet.stream().map(stringStringMap -> {
                                        // 拼接缓存的键
                                        String tmpMethodName = stringStringMap.get("m");
                                        String s = stringStringMap.get("a");
                                        String spELArg = s.substring(0, s.length() - 1);

                                        // 缓存spEL
                                        return AspectTools.joiningKey(basePackage, simpleClassName, tmpMethodName, AspectTools.joiningKey(stringMap.get(tmpMethodName)), spELArg) + AspectTools.ARGS_JOINT_MARK_FUZZY;

                                    }).filter(s -> s.length() > 0).collect(Collectors.toList());

                                    // 缓存spEL
                                    spELCacheLists.addAll(collect);
                                }

                                logger.info("===============>spELCacheLists:{}", spELCacheLists);

                                // TODO 将结果缓存在内存中 存入List<String>
                                CACHE.put(evictCacheKey, spELCacheLists);
                            }
                        }
                    }

                }
            }
        } catch (Exception e) {
            logger.error("===============>e:{}\n", e);
        }

        return bean;
    }

}