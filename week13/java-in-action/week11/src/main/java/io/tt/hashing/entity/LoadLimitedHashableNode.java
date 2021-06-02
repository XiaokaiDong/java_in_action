package io.tt.hashing.entity;

import io.tt.hashing.exception.TooBusyException;
import io.tt.hashing.management.ConsistentHashingDataService;
import io.tt.hashing.management.HashingDataServiceAware;

import java.util.function.Predicate;

/**
 * 有存储上限的存储节点，用于实现“带有限负载的一致性哈希”。
 * 当数据按照一致性哈希算法找到相应的存储节点时，要先判断该存储节点是否达到了存储上限；
 * 如果已经达到了存储上限，则需要继续寻找该节点顺时针方向之后的节点进行存储.
 * 所谓的Ordered是指寻找下一节点的次数，比如寻找一次就是一阶，等等。
 *
 * 寻找的动作由上层的Service来进行。寻找的次数由"阶数Ordered"指定。
 * 所谓的Ordered是指寻找下一节点的次数，比如寻找一次就是一阶，等等。
 */
public class LoadLimitedHashableNode implements HashableNode, HashingDataServiceAware {

    /**
     * 预测是否达到上限
     */
    private final Predicate<LoadLimitedHashableNode> canTakeMoreData;

    private ConsistentHashingDataService dataService;

    /**
     * 对应的物理节点，用于真正保存数据
     */
    final private HashableNode originalNode;

    //本节点的编号
    private int nodeNo;

    public LoadLimitedHashableNode(Predicate<LoadLimitedHashableNode> canTakeMoreData,
                                   HashableNode originalNode) {
        this.canTakeMoreData = canTakeMoreData;
        this.originalNode = originalNode;
    }

    /**
     *
     * @param data 被存储的数据
     * @throws TooBusyException, 当无法找到更多空间时抛出这种异常。上层Service捕获后，
     * 寻找哈希环上下一个节点。
     */
    @Override
    public void put(Hashable data) {
        if (canTakeMoreData.test(this)) {
            originalNode.put(data);
        } else {
            throw new TooBusyException("空间不足");
        }
    }

    public boolean canTakeOne() {
        return canTakeMoreData.test(this);
    }

    @Override
    public Hashable get(int key) {
        return originalNode.get(key);
    }

    @Override
    public void remove(int key) {
        originalNode.remove(key);
    }

    @Override
    public void merge(HashableNode otherNode) {
        originalNode.merge(otherNode);
    }

    @Override
    public void exportDataTo(HashableNode otherNode) {
        originalNode.exportDataTo(otherNode);
    }

    @Override
    public int getKey() {
        return originalNode.getKey();
    }

    @Override
    public void setDataServiceAware(ConsistentHashingDataService dataService) {
        this.dataService = dataService;
    }
}
