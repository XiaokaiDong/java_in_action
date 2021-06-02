package io.tt.hashing.entity;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 虚拟一致性哈希节点实现
 * 用于支持虚拟节点层，以提高数据的均衡性
 * 在真实的数据节点与哈希环之间引入一个虚拟节点层
 */
public class VirtualHashableNode implements HashableNode{

    /**
     * 用来保存数据的键值，完整的数据存储在物理节点上
     * 使用了CopyOnWriteArraySet，表示默认情况下读多写少。此处可以更加的抽象
     */
    Set<Integer> dataSlot = new CopyOnWriteArraySet<>();

    /**
     * 虚拟节点均匀的分布在哈希环上。哈希环对应着32位整型哈希值，所以虚拟节点的哈希值
     * 是创建哈希环时指定的
     */
    private int nodeNo;

    /**
     * 对应的物理节点，用于真正保存数据
     */
    final private HashableNode originalNode;

    /**
     * 创建时必须指定实际保存数据的“源节点”
     * @param originalNode
     */
    public VirtualHashableNode(HashableNode originalNode, int nodeNo) {
        this.originalNode = originalNode;
        this.nodeNo = nodeNo;
    }


    public HashableNode getOriginalNode() {
        return originalNode;
    }

    @Override
    public void put(Hashable data) {
        //保存数据的键值
        dataSlot.add(data.getKey());
        //真正存储数据
        originalNode.put(data);
    }

    @Override
    public Hashable get(int key) {
        Hashable result = null;
        if(dataSlot.contains(key))
            result = originalNode.get(key);

        return result;
    }

    @Override
    public void merge(HashableNode otherNode) {
        //虚拟节点不支持合并
        return;
    }

    @Override
    public void exportDataTo(HashableNode otherNode) {
        for (int key : dataSlot) {
            //先将数据保存到目标节点
            otherNode.put(get(key));
            //然后从当前节点上删除此条数据
            //  先逻辑删除
            dataSlot.remove(key);
            //  再物理删除
            originalNode.remove(key);
        }
    }


    @Override
    public int getKey() {
        return nodeNo;
    }

    @Override
    public void remove(int key) {
        dataSlot.remove(key);
        originalNode.remove(key);
    }
}
