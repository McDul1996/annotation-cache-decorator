package com.github.hcsp.annotation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

public class CacheClassDecorator {
    // 将传入的服务类Class进行增强
    // 使得返回一个具有如下功能的Class：
    // 如果某个方法标注了@Cache注解，则返回值能够被自动缓存注解所指定的时长
    // 这意味着，在短时间内调用同一个服务的同一个@Cache方法两次
    // 它实际上只被调用一次，第二次的结果直接从缓存中获取
    // 注意，缓存的实现需要是线程安全的
    @SuppressWarnings("unchecked")
    public static <T> Class<T> decorate(Class<T> klass) {
        return (Class<T>) new ByteBuddy()
                // 将类进行子类化
                .subclass(klass)
                // 使用元素匹配器将里面的方法进行一次拦截
                .method(ElementMatchers.isAnnotatedWith(Cache.class))
                // 将一份带有@RuntimeType的方法说明书交给方法委托器来处理这些拦截的方法
                .intercept(MethodDelegation.to(CacheAdvisor.class))
                // 创建动态类
                .make()
                // 将这个动态类丢给父类加载器
                .load(klass.getClassLoader())
                // 加载
                .getLoaded();
    }


    // 这个注解的目的是当拦截发生的时候，ByteBuddy能够找到匹配的方法来调用
    // 这个委托发生的时候，我可以从这里拿到当前正在被调用的实例方法
    // 哪个对象在调用这个方法
    // 获得拦截方法需要传入的所有参数

    public static void main(String[] args) throws Exception {
        DataService dataService = decorate(DataService.class).getConstructor().newInstance();

        // 有缓存的查询：只有第一次执行了真正的查询操作，第二次从缓存中获取
        System.out.println(dataService.queryData(1));
        Thread.sleep(1 * 1000);
        System.out.println(dataService.queryData(1));

        // 无缓存的查询：两次都执行了真正的查询操作
        System.out.println(dataService.queryDataWithoutCache(1));
        Thread.sleep(1 * 1000);
        System.out.println(dataService.queryDataWithoutCache(1));
    }


}
