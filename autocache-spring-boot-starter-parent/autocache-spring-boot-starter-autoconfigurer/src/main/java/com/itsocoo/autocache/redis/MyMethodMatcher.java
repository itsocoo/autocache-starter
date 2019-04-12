package com.itsocoo.autocache.redis;

import org.springframework.aop.MethodMatcher;

import java.lang.reflect.Method;

public class MyMethodMatcher implements MethodMatcher {
    public boolean matches(Method m, Class targetClass) {
        return m.getName().equals("buy");
    }

    public boolean isRuntime() {
        return false;
    }

    public boolean matches(Method m, Class target, Object[] args) {
        return true;
    }
}
