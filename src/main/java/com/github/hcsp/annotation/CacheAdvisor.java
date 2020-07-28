package com.github.hcsp.annotation;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class CacheAdvisor {

    private static ConcurrentHashMap<CacheKey,CacheValue> cache = new ConcurrentHashMap<>();
    @RuntimeType
    public static Object cache(
            // 直接调用父类里的此方法
            @SuperCall Callable<Object> superCall,
            @Origin Method method,
            @This Object thisObject,
            @AllArguments Object[] arguments) {
        CacheKey cacheKey = new CacheKey(thisObject, method.getName(), arguments);
        final CacheValue cacheValue = cache.get(cacheKey);
        if (cacheValue != null) {
            if (isOverCachetSeconds(cacheValue,method)) {

                return getRealObject(method, cacheKey);
            }
                return cacheValue.getObject();
            }
        return getRealObject(method, cacheKey);
    }

    private static boolean isOverCachetSeconds(CacheValue cacheValue, Method method) {
        long time = cacheValue.getTime();
        int cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds();
        return System.currentTimeMillis() - time > cacheSeconds * 1000;
    }

    private static Object getRealObject(Method method, CacheKey cacheKey) {
        Object realInvokeMethodResult = null;
        try {
            realInvokeMethodResult = method.invoke(cacheKey.getObject(), cacheKey.getArguments());
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        cache.put(cacheKey, new CacheValue(realInvokeMethodResult, System.currentTimeMillis()));
        return realInvokeMethodResult;
    }
}
