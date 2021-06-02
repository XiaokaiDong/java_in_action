package io.tt.cache;

import io.tt.cache.registry.CacheOperationRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.annotation.SpringCacheAnnotationParser;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CacheOperationSource;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CacheManager工厂类
 */
public class CacheManagerResolver implements CacheOperationSource, CacheOperationRegistry {

    private final CacheOperationSource cacheOperationSource =
            new AnnotationCacheOperationSource(new SpringCacheAnnotationParser());

    private final Map<String, CacheOperation> cacheOperationRegistry = new ConcurrentHashMap<>();


    @Override
    public boolean isCandidateClass(Class<?> targetClass) {
        return cacheOperationSource.isCandidateClass(targetClass);
    }

    @Override
    public Collection<CacheOperation> getCacheOperations(Method method, Class<?> targetClass) {
        return cacheOperationSource.getCacheOperations(method, targetClass);
    }

    public CacheManager getCacheManager(){
        RedisCacheManager redisCacheManager = new RedisCacheManager("");
        redisCacheManager.setCacheOperations(getCacheOperations());
        return redisCacheManager;
    }

    @Override
    public void addCacheOperations(Collection<CacheOperation> cacheOperations) {
        for (CacheOperation cacheOperation: cacheOperations) {
            if (!cacheOperationRegistry.containsKey(cacheOperation.getName())) {
                cacheOperationRegistry.putIfAbsent(cacheOperation.getName(), cacheOperation);
            }
        }
    }

    @Override
    public Collection<CacheOperation> getCacheOperations() {
        return cacheOperationRegistry.values();
    }

    public CacheOperation getCacheOperation(String name) {
        return cacheOperationRegistry.get(name);
    }
}
