package io.tt.hashing.entity;

import io.tt.hashing.exception.TooBusyException;

import java.util.NoSuchElementException;

/**
 * 可以放在哈希环上的数据存储节点
 * 可是是物理的，也可能是虚拟的节点
 */
public interface HashableNode extends Hashable{

    /**
     * 存储数据
     * @param data 被存储的数据
     * @throws TooBusyException 当节点压力太大时（比如达到了数据存储的上限），抛出
     *         此异常，可以用于带负载的一致性哈希
     */
    void put(Hashable data);

    /**
     * 读取数据
     * @param key 数据的键值
     * @return 目标数据
     * @throws NoSuchElementException 当节点上不存在目标数据时，抛出此异常
     */
    Hashable get(int key);

    /**
     * 移除一条数据
     * @param key 待移除数据的键值
     */
    void remove(int key);

    /**
     * 将这个数据节点与另外一个节点otherNode合并，otherNode上的数据会被迁移到本节点
     * @param otherNode 被合并的节点
     */
    void merge(HashableNode otherNode);

    /**
     * 根据一致性哈希算法，将自己保存的数据移动到目标节点上。
     * 可能没有数据需要移动。数据移动后并不是立即删除，而是采取一定的策略进行删除。
     * @param otherNode 目标数据节点
     */
    void exportDataTo(HashableNode otherNode);

}
