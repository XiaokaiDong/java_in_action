package io.tt.hashing.management;

/**
 * 用于注入哈希数据服务
 */
public interface HashingDataServiceAware {
    void setDataServiceAware(ConsistentHashingDataService dataService);
}
