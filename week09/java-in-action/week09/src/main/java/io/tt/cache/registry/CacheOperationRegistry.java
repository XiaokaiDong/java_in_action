package io.tt.cache.registry;

import org.springframework.cache.interceptor.CacheOperation;

import java.util.Collection;

/**
 * 管理所有的CacheOperation
 */
public interface CacheOperationRegistry {

    /**
     * 添加 CacheOperation。需要做到去重
     * @param cacheOperations 需要被添加的CacheOperation
     */
    void addCacheOperations(Collection<CacheOperation> cacheOperations);

    /**
     * 返回所有的CacheOperation对象
     * @return CacheOperation对象
     */
    Collection<CacheOperation> getCacheOperations();
}
