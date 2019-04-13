package com.itsocoo.autocache.redis.test.controller;

import com.itsocoo.autocache.redis.test.entity.TestCacheEntityParams;
import com.itsocoo.autocache.redis.test.service.ITestCacheEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wanghaibo
 * @version V1.0
 * @desc
 * @date 2019/1/14 15:23
 */
@RestController
@RequestMapping("test")
public class TestCacheEntityController {
    private final ITestCacheEntityService testCacheEntityService;

    @Autowired
    public TestCacheEntityController(ITestCacheEntityService testCacheEntityService) {
        this.testCacheEntityService = testCacheEntityService;
    }

    @GetMapping
    public String testQuery() {
        Long id = 1L;

        String brand = "only1";
        String orderId = "11111";
        String name = "whb1";

        Map<String, Object> params = new HashMap<>();
        params.put("brand", brand);
        params.put("orderId", orderId);
        params.put("name", name);
        params.put("id", id);

        TestCacheEntityParams testCacheEntityParams = new TestCacheEntityParams();
        testCacheEntityParams.setBrand(brand);
        testCacheEntityParams.setOrderId(orderId);
        testCacheEntityParams.setName(name);
        testCacheEntityParams.setPage(0);
        testCacheEntityParams.setPageSize(0);
        testCacheEntityParams.setOrderBy("");

        testCacheEntityService.find(id);

        testCacheEntityService.findOneById(id);

        testCacheEntityService.findOneByBrandAndOrderId(brand, orderId);

        testCacheEntityService.findOneByBrandAndOrderIdAndName(params);

        testCacheEntityService.list(id, name);

        testCacheEntityService.lists(params);

        testCacheEntityService.listAlls(testCacheEntityParams);

        testCacheEntityService.listAlls2(id, params, testCacheEntityParams);

        testCacheEntityService.listAlls3(id, params, testCacheEntityParams);

        return "";
    }

    @PutMapping
    public String testEvict() {

        Long id = 1L;
        String brand = "only1";
        String orderId = "11111";
        String name = "whb1";

        Map<String, Object> params = new HashMap<>();
        params.put("brand", brand);
        params.put("orderId", orderId);
        params.put("name", name);
        params.put("id", id);

        TestCacheEntityParams testCacheEntityParams = new TestCacheEntityParams();
        testCacheEntityParams.setBrand(brand);
        testCacheEntityParams.setOrderId(orderId);
        testCacheEntityParams.setName(name);

        testCacheEntityService.update(id);
        testCacheEntityService.update1(params);
        testCacheEntityService.update2(testCacheEntityParams);
        testCacheEntityService.update2_2(id);

        testCacheEntityService.update3(id, params, testCacheEntityParams);

        testCacheEntityService.update4(id, params, testCacheEntityParams);

        return "";
    }

}
