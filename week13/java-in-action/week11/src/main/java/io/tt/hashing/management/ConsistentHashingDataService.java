package io.tt.hashing.management;

import io.tt.hashing.entity.Hashable;
import io.tt.hashing.entity.HashableNode;
import io.tt.hashing.exception.IncompatibleTypeException;
import io.tt.hashing.exception.TooBusyException;

/**
 * 存储节点以及数据的管理器
 */
public interface ConsistentHashingDataService extends Router{

    /**
     * 注册一个节点。
     * 新的节点注册进来后，会引发数据的重新分布
     * @param dataNode 被注册的数据节点
     * @throws IncompatibleTypeException 当注册带有限负载的节点时，需要此Service的阶数大于0
     */
    void register(HashableNode dataNode);

    /**
     * 注销一个数据节点
     * 节点注销后，其上的数据需要分布到其它几点
     * @param dataNode 被注销的数据节点
     */
    void deRegister(HashableNode dataNode);

    /**
     * 存储数据
     * @param data 数据
     * @throws TooBusyException 无法找到空间时抛出此异常
     */
    void put(Hashable data);

    /**
     * 获取数据
     * @param key 目标数据的键值
     * @return 数据
     */
    Hashable get(int key);

    /**
     * 得到哈希环上指定节点的下一个节点
     * @param dataNode 当前节点
     * @return 下一个节点
     */
    HashableNode getNextNodeGreaterThan(HashableNode dataNode);

    /**
     * 得到哈希环上大于等于指定键的下一个节点
     * @param startKey 当前节点的键值
     * @return 下一个节点
     */
    HashableNode getNextNodeEqualOrGreaterThan(Integer startKey);

    /**
     * 得到阶数。
     * <li>如果为0，则表示不支持带有限负载的一致性哈希。</li>
     * <li>如果大于0，则表示支持带有限负载的一致性哈希。</li>
     * @return 阶数
     */
    int getOrder();
}
