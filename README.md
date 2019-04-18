---
title: 集成Redis自动缓存和缓存细粒度删除工具
date: 2019-04-18
categories: ['心得']
tags: ['心得','分享']
comments: true
---

### 原理

>项目启动时候在配置了``itsocoo.cache.aspect.scan-package-paths``时候会自动扫描需要缓存的方法并生成缓存的KEY
>执行方法的时候AOP切入改方法，存在KEY在直接拼接后缓存、删除。否则会生成KEY，在缓存、删除

### 引入pom

```xml
<dependency>
    <groupId>com.itsocoo</groupId>
    <artifactId>autocache-spring-boot-starter</artifactId>
    <version>2.1.3.RELEASE</version>
</dependency>
```

### 配置

```properties
# 启动自动缓存的AOP配置
itsocoo.cache.aspect.enable=true
# 自动扫描缓存的类中方法前缀
itsocoo.cache.aspect.pointcut-query=find,list,load
# 删除缓存的方法前缀
itsocoo.cache.aspect.pointcut-evict=update,delete,clean
# 不写scan-package-paths 则启动项目的时候不扫描符合条件的类
itsocoo.cache.aspect.scan-package-paths=com.itsocoo.autocache.redis.test.service.impl
# 缓存的具体类后缀名称
itsocoo.cache.aspect.scan-class-suffix=ServiceImpl
```

### 代码约定配置

> com.itsocoo.autocache.redis.common.AspectTools

```java
    // 约定spEll的参数的连接符
    public final static String ARGS_JOINT_MARK = ".";
    // 约定spEll的参数的前缀模糊查询标识
    public final static String ARGS_JOINT_MARK_FUZZY = "*";
    // 约定spEll的参数的自动查询标识
    public final static String ARGS_JOINT_MARK_AUTO = "#";
    // 解析方法的名称
    public static final String PARSE_SPLIT_PREFIX = "By";
    // 解析方法的名称的连接符
    public static final String PARSE_SPLIT_CONNECT = "And";
    // 删除的方法分隔符
    public static final String CACHE_EVICT_VALUES_SEPARATOR = ":";
```

> 缓存的方式：
> 解析方法名称和方法参数
>
> 1.有By的则解析By...And...的参数名称拼接缓存
> 2.没有的则直接拼接该方法的所有参数名称 

### 测试缓存

```java
 @Override
    public TestCacheEntity find(Long id) {
        List<TestCacheEntity> collect = entityList.stream().filter(testCacheEntity -> testCacheEntity.getId() == id).collect(Collectors.toList());
        return collect.get(0);
    }

    @Override
    public TestCacheEntity findOneById(Long id) {
        List<TestCacheEntity> collect = entityList.stream().filter(testCacheEntity -> testCacheEntity.getId() == id).collect(Collectors.toList());
        return collect.get(0);
    }

    @Override
    public TestCacheEntity findOneByBrandAndOrderId(String brand, String orderId) {
        List<TestCacheEntity> collect = entityList.stream().filter(testCacheEntity -> testCacheEntity.getBrand().equalsIgnoreCase(brand) && testCacheEntity.getOrderId().equalsIgnoreCase(orderId)).collect(Collectors.toList());
        return collect.get(0);
    }

    @Override
    public TestCacheEntity findOneByBrandAndOrderIdAndName(Map<String, Object> params) {
        String brand = (String) params.get("brand");
        String orderId = (String) params.get("orderId");
        String name = (String) params.get("name");

        List<TestCacheEntity> collect = entityList.stream().filter(testCacheEntity -> testCacheEntity.getBrand().equalsIgnoreCase(brand) && testCacheEntity.getOrderId().equalsIgnoreCase(orderId) && testCacheEntity.getName().equalsIgnoreCase(name)).collect(Collectors.toList());
        return collect.get(0);
    }

    @Override
    public List<TestCacheEntity> list(Long id, String name) {
        Runnable runnable = () -> {
        };
        return entityList;
    }

    @Override
    public List<TestCacheEntity> lists(Map<String, Object> params) {
        return entityList;
    }

    @Override
    public List<TestCacheEntity> listAlls(TestCacheEntityParams testCacheEntityParams) {
        return entityList;
    }

    @Override
    public List<TestCacheEntity> listAlls2(Long id, Map<String, Object> objectMap, TestCacheEntityParams testCacheEntityParams) {
        return entityList;
    }

    @Override
    public List<TestCacheEntity> listAlls3(Long id, Map<String, Object> objectMap, TestCacheEntityParams testCacheEntityParams) {
        return entityList;
    }
```

> 删除缓存的方式：
> 注意下面@CacheEvict的写法
> 解析方法名称和方法参数,支持下面4中方式删除缓存
>
> // 1.自动查询删除
> // @RedisCacheEvict(value = {"findDetailCacheByOrderId", "findOneCacheById", "findOneCacheByIdAndBrandAndName"})
> // 2.前缀模糊删除
> // @RedisCacheEvict(value = {"list:*"})
> // 2_2.前缀组合模糊删除
> // @RedisCacheEvict(value = {"list:#objectMap['id']*"})
> // 3.精确查询删除
> // @RedisCacheEvict(value = {"findOneCacheById:#objectMap['id']"})
> // 4.混合查询删除: * 代表该方法采用前缀模糊删除; ""或者"#" 代表自动查询(根据约定的格式解析方法名称);
> // 测试参数是基本类型+实体+Map

### 测试删除

```java

    @Override
    @RedisCacheEvict({"find:#id", "findOneById"})
    public int update(Long id) {
        return 0;
    }

    // TODO: 该种情况必须list:#params['id'].#params['name'] 使用“[]” 多个之间使用“.”号连接
    @Override
    @RedisCacheEvict({"findOneByBrandAndOrderId", "findOneByBrandAndOrderIdAndName", "list:#params['id'].#params['name']"})
    public int update1(Map<String, Object> params) {
        return 0;
    }

    @Override
    @RedisCacheEvict({"listAlls:*", "listAlls2:*"})
    public int update2(TestCacheEntityParams testCacheEntityParams) {
        return 0;
    }


    @Override
    @RedisCacheEvict({"listAlls3:#id*"})
    public int update2_2(Long id) {
        return 0;
    }

    // TODO: 注意该种情况下lists和find的参数名称必须和update3一样 例如 lists的params; find的id
    // 删除
    @Override
    @RedisCacheEvict({"lists", "find"})
    public int update3(Long id, Map<String, Object> params, TestCacheEntityParams testCacheEntityParams) {
        return 0;
    }

    // 删除所有
    @Override
    @RedisCacheEvict({"find", "findOneById", "findOneByBrandAndOrderId", "findOneByBrandAndOrderIdAndName", "list:#params['id'].#params['name']", "lists", "listAlls:*", "listAlls2:*"})
    // @RedisCacheEvict({"findOneById", "findOneByBrandAndOrderId", "findOneByBrandAndOrderIdAndName"})
    // @RedisCacheEvict({"listAlls:*", "listAlls2:*"})
    // @RedisCacheEvict({"list:#params['id'].#params['name']"})
    // @RedisCacheEvict({"find","lists"})
    public int update4(Long id, Map<String, Object> params, TestCacheEntityParams testCacheEntityParams) {
        return 0;
    }
}
```

**具体见[代码](https://github.com/itsocoo/autocache-starter)**



