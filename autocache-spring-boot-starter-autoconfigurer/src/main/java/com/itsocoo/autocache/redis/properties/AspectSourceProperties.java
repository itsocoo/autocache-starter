package com.itsocoo.autocache.redis.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wanghaibo
 * @version V1.0
 * @desc
 * @date 2018/8/7 16:03
 */
@ConfigurationProperties(prefix = AspectSourceProperties.ASPECT_SOURCE_PREFIX)
public class AspectSourceProperties {
    static final String ASPECT_SOURCE_PREFIX = "lzsz.cache.aspect";

    // 是否开启该自动缓存
    private boolean enable;


    // 缓存的切点
    private String pointcutQuery = "find,list,load";

    // 删除缓存的切点
    private String pointcutEvict = "update,delete,clean";

    // 要扫描的包路径
    private String[] scanPackagePaths;

    // 要扫描的类的后缀
    private String scanClassSuffix="ServiceImpl";

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getPointcutQuery() {
        return pointcutQuery;
    }

    public void setPointcutQuery(String pointcutQuery) {
        this.pointcutQuery = pointcutQuery;
    }

    public String getScanClassSuffix() {
        return scanClassSuffix;
    }

    public void setScanClassSuffix(String scanClassSuffix) {
        this.scanClassSuffix = scanClassSuffix;
    }

    public String getPointcutEvict() {
        return pointcutEvict;
    }

    public void setPointcutEvict(String pointcutEvict) {
        this.pointcutEvict = pointcutEvict;
    }

    public String[] getScanPackagePaths() {
        return scanPackagePaths;
    }

    public void setScanPackagePaths(String[] scanPackagePaths) {
        this.scanPackagePaths = scanPackagePaths;
    }
}
