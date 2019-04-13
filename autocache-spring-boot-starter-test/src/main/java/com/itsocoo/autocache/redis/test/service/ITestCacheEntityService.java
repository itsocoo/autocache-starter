package com.itsocoo.autocache.redis.test.service;

import com.itsocoo.autocache.redis.test.entity.TestCacheEntity;
import com.itsocoo.autocache.redis.test.entity.TestCacheEntityParams;

import java.util.List;
import java.util.Map;

/**
 * @author wanghaibo
 * @version V1.0
 * @desc
 * @date 2019/1/14 14:55
 */
public interface ITestCacheEntityService {

    TestCacheEntity find(Long id);

    TestCacheEntity findOneById(Long id);

    TestCacheEntity findOneByBrandAndOrderId(String brand, String orderId);

    TestCacheEntity findOneByBrandAndOrderIdAndName(Map<String, Object> params);

    List<TestCacheEntity> list(Long id, String name);

    List<TestCacheEntity> lists(Map<String, Object> params);

    List<TestCacheEntity> listAlls(TestCacheEntityParams testCacheEntityParams);

    List<TestCacheEntity> listAlls2(Long id, Map<String, Object> objectMap, TestCacheEntityParams testCacheEntityParams);

    List<TestCacheEntity> listAlls3(Long id, Map<String, Object> objectMap, TestCacheEntityParams testCacheEntityParams);

    int update(Long id);

    int update1(Map<String, Object> params);

    int update2(TestCacheEntityParams testCacheEntityParams);

    int update2_2(Long id);

    int update3(Long id, Map<String, Object> objectMap, TestCacheEntityParams testCacheEntityParams);

    int update4(Long id, Map<String, Object> objectMap, TestCacheEntityParams testCacheEntityParams);


}
