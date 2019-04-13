package com.itsocoo.autocache.redis.test.entity;

import java.io.Serializable;

/**
 * @author wanghaibo
 * @version V1.0
 * @desc 测试缓存的实体
 * @date 2018/12/11 11:30
 */
public class TestCacheEntity implements Serializable {

    private static final long serialVersionUID = 5687082086144060154L;
    private Long id;
    private String brand;
    private String orderId;
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TestCacheEntity() {
    }

    public TestCacheEntity(Long id, String brand, String orderId, String name) {
        this.id = id;
        this.brand = brand;
        this.orderId = orderId;
        this.name = name;
    }

    @Override
    public String toString() {
        return "TestCacheEntity{" +
                "id=" + id +
                ", brand='" + brand + '\'' +
                ", orderId='" + orderId + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
