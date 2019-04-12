package com.itsocoo.autocache.redis.common;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wanghaibo
 * @version V1.0
 * @desc 要验证@RedisCacheEvict方法的value是否真实存在于方法中并且去掉不存在的方法
 * @date 2019/1/11 17:55
 */

// methods = checkDeclaredMethods.getMethods();
// Map<String, String[]> stringMap = checkDeclaredMethods.getStringMap();
// TODO: 因为是要返回上面的2个值 所以使用Class 来定义 后期可以改为guava Table
/*Table<String, String, String[]> aTable = HashBasedTable.create();
// 同时获取方法的参数名称
for (Method declaredMethod : aClassDeclaredMethods) {
    String name = declaredMethod.getName();
    for (String s : methods) {
        if (s.startsWith(name)) {
            aTable.put(s, name, u.getParameterNames(declaredMethod));
            break;
        }
    }
}
// 使用过滤后的值
methods = aTable.rowKeySet().stream().toArray(String[]::new);

Map<String, String[]> stringMap = aTable.rowMap().values().stream().reduce((strings, strings2) -> {
    Map<String, String[]> map = new HashMap<>();
    map.putAll(strings);
    map.putAll(strings2);
    return map;
}).orElse(new HashMap<>());*/
public class CheckDeclaredMethods {
    private Class<?> aClass;
    private String[] methods;
    private LocalVariableTableParameterNameDiscoverer u;
    private Map<String, String[]> stringMap;


    public CheckDeclaredMethods(Class<?> aClass, String[] methods, LocalVariableTableParameterNameDiscoverer u) {
        this.aClass = aClass;
        this.methods = methods;
        this.u = u;
    }

    private CheckDeclaredMethods(Builder builder) {
        aClass = builder.aClass;
        methods = builder.methods;
        u = builder.u;
        stringMap = builder.stringMap;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String[] getMethods() {
        return methods;
    }

    public Map<String, String[]> getStringMap() {
        return stringMap;
    }

    public CheckDeclaredMethods invoke() {
        Method[] aClassDeclaredMethods = aClass.getDeclaredMethods();

        List<String> asList = new ArrayList();
        stringMap = new HashMap<>();
        // 同时获取方法的参数名称
        for (Method declaredMethod : aClassDeclaredMethods) {
            String name = declaredMethod.getName();
            for (String s : methods) {
                if (s.startsWith(name)) {
                    asList.add(s);
                    String[] uParameterNames = u.getParameterNames(declaredMethod);
                    stringMap.put(name, uParameterNames);
                    break;
                }
            }
        }
        // 使用过滤后的值
        methods = asList.stream().toArray(String[]::new);
        return this;
    }

    public static final class Builder {
        private Class<?> aClass;
        private String[] methods;
        private LocalVariableTableParameterNameDiscoverer u;
        private Map<String, String[]> stringMap;

        private Builder() {
        }

        public Builder withAClass(Class<?> val) {
            aClass = val;
            return this;
        }

        public Builder withMethods(String[] val) {
            methods = val;
            return this;
        }

        public Builder withU(LocalVariableTableParameterNameDiscoverer val) {
            u = val;
            return this;
        }

        public Builder withStringMap(Map<String, String[]> val) {
            stringMap = val;
            return this;
        }

        public CheckDeclaredMethods build() {
            return new CheckDeclaredMethods(this).invoke();
        }
    }
}