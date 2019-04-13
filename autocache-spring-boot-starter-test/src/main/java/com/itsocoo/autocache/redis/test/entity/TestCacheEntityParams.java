package com.itsocoo.autocache.redis.test.entity;

import java.io.Serializable;

/**
 * @author wanghaibo
 * @version V1.0
 * @desc
 * @date 2019/1/14 15:46
 */
public class TestCacheEntityParams implements Serializable {

    private static final long serialVersionUID = 5296771985624412328L;
    private String brand;
    private String orderId;
    private String name;
    private Integer page = 1;
    private Integer pageSize = 20;
    private String orderBy;

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

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public TestCacheEntityParams() {
    }

    @Override
    public String toString() {
        return "TestCacheEntityParams{" +
                "brand='" + brand + '\'' +
                ", orderId='" + orderId + '\'' +
                ", name='" + name + '\'' +
                ", page=" + page +
                ", pageSize=" + pageSize +
                ", orderBy='" + orderBy + '\'' +
                '}';
    }
}
