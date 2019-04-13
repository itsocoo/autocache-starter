package com.itsocoo.autocache.redis.test.service.impl;

import com.itsocoo.autocache.redis.annotation.RedisCacheEvict;
import com.itsocoo.autocache.redis.test.entity.TestCacheEntity;
import com.itsocoo.autocache.redis.test.entity.TestCacheEntityParams;
import com.itsocoo.autocache.redis.test.service.ITestCacheEntityService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author wanghaibo
 * @version V1.0
 * @desc
 * @date 2019/1/14 14:57
 */
@Service
public class TestCacheEntityServiceImpl implements ITestCacheEntityService {

    // TODO: 方法里面有lambda 在getDeclaredMethods()后获取的方法里面会产生很多的静态"lambda$xxx$"方法 要过滤掉

    private static List<TestCacheEntity> datas = new ArrayList<>();

    static {
        datas.add(new TestCacheEntity(1L, "only1", "11111", "whb1"));
        datas.add(new TestCacheEntity(2L, "only2", "22222", "whb2"));
        datas.add(new TestCacheEntity(3L, "only3", "33333", "whb3"));
        datas.add(new TestCacheEntity(4L, "only4", "44444", "whb4"));
        datas.add(new TestCacheEntity(5L, "only5", "55555", "whb5"));
        datas.add(new TestCacheEntity(6L, "only6", "66666", "whb6"));
        datas.add(new TestCacheEntity(7L, "only7", "77777", "whb7"));
    }

    // TODO: 注意下面缓存的写法
    // 1.有By的则解析By...And...的参数名称拼接缓存
    // 2.没有的则直接拼接该方法的所有参数名称

    @Override
    public TestCacheEntity find(Long id) {
        List<TestCacheEntity> collect = datas.stream().filter(testCacheEntity -> testCacheEntity.getId() == id).collect(Collectors.toList());
        return collect.get(0);
    }

    @Override
    public TestCacheEntity findOneById(Long id) {
        List<TestCacheEntity> collect = datas.stream().filter(testCacheEntity -> testCacheEntity.getId() == id).collect(Collectors.toList());
        return collect.get(0);
    }

    @Override
    public TestCacheEntity findOneByBrandAndOrderId(String brand, String orderId) {
        List<TestCacheEntity> collect = datas.stream().filter(testCacheEntity -> testCacheEntity.getBrand().equalsIgnoreCase(brand) && testCacheEntity.getOrderId().equalsIgnoreCase(orderId)).collect(Collectors.toList());
        return collect.get(0);
    }

    @Override
    public TestCacheEntity findOneByBrandAndOrderIdAndName(Map<String, Object> params) {
        String brand = (String) params.get("brand");
        String orderId = (String) params.get("orderId");
        String name = (String) params.get("name");

        List<TestCacheEntity> collect = datas.stream().filter(testCacheEntity -> testCacheEntity.getBrand().equalsIgnoreCase(brand) && testCacheEntity.getOrderId().equalsIgnoreCase(orderId) && testCacheEntity.getName().equalsIgnoreCase(name)).collect(Collectors.toList());
        return collect.get(0);
    }

    @Override
    public List<TestCacheEntity> list(Long id, String name) {
        Runnable runnable = () -> {
        };
        return datas;
    }

    @Override
    public List<TestCacheEntity> lists(Map<String, Object> params) {
        return datas;
    }

    @Override
    public List<TestCacheEntity> listAlls(TestCacheEntityParams testCacheEntityParams) {
        return datas;
    }

    @Override
    public List<TestCacheEntity> listAlls2(Long id, Map<String, Object> objectMap, TestCacheEntityParams testCacheEntityParams) {
        return datas;
    }

    @Override
    public List<TestCacheEntity> listAlls3(Long id, Map<String, Object> objectMap, TestCacheEntityParams testCacheEntityParams) {
        return datas;
    }

    // TODO: 注意下面@CacheEvict的写法
    // 1.自动查询删除
    // @RedisCacheEvict(value = {"findDetailCacheByOrderId", "findOneCacheById", "findOneCacheByIdAndBrandAndName"})
    // 2.前缀模糊删除
    // @RedisCacheEvict(value = {"list:*"})
    // 2_2.前缀组合模糊删除
    // @RedisCacheEvict(value = {"list:#objectMap['id']*"})
    // 3.精确查询删除
    // @RedisCacheEvict(value = {"findOneCacheById:#objectMap['id']"})
    // 5.混合查询删除: * 代表该方法采用前缀模糊删除; ""或者"#" 代表自动查询(根据约定的格式解析方法名称);
    // 测试参数是基本类型+实体+Map


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
