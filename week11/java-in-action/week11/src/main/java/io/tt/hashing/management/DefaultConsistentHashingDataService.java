package io.tt.hashing.management;

import io.tt.hashing.entity.Hashable;
import io.tt.hashing.entity.HashableNode;
import io.tt.hashing.entity.LoadLimitedHashableNode;
import io.tt.hashing.exception.TooBusyException;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * ConsistentHashingDataManager的缺省实现类.
 * 支持带负载均衡的一致性哈希。此时order大于0，节点类型为LoadLimitedHashableNode
 * 当order == 0时：
 * <li>如果节点为普通HashableNode，则为一般的一致性哈希算法</li>
 * <li>如果节点为普通VirtualHashableNode，则为带虚拟节点的一致性哈希算法</li>
 */
public class DefaultConsistentHashingDataService implements ConsistentHashingDataService {

    private final int order;

    /**
     * 哈希环，使用Collections.synchronizedSortedMap保证线程安全
     */
    private final SortedMap<Integer, HashableNode> registry =
            Collections.synchronizedSortedMap(new TreeMap<>());

    public DefaultConsistentHashingDataService(int order) {
        this.order = order;
    }

    @Override
    public void register(HashableNode dataNode) {
        if(order > 0 && !(dataNode instanceof LoadLimitedHashableNode))
            throw new IncompatibleClassChangeError("阶数大于0时，只能注册带有限负载节点!");

        registry.putIfAbsent(dataNode.getKey(), dataNode);

        //找到哈希环上位于自己逆时针方向之后的第一个节点
        HashableNode nextNode = getNextNodeGreaterThan(dataNode);

        //迁移数据
        nextNode.exportDataTo(dataNode);
    }

    @Override
    public HashableNode getNextNodeGreaterThan(HashableNode dataNode) {
        HashableNode nextNode = null;
        SortedMap<Integer, HashableNode> tailMap = registry.tailMap(dataNode.getKey());
        for (Integer key : tailMap.keySet()) {
            if (key > dataNode.getKey()){
                nextNode = tailMap.get(key);
                break;
            }
        }
        return nextNode;
    }

    @Override
    public HashableNode getNextNodeEqualOrGreaterThan(Integer startKey) {
        HashableNode nextNode = null;
        SortedMap<Integer, HashableNode> tailMap = registry.tailMap(startKey);
        for (Integer key : tailMap.keySet()) {
            nextNode = tailMap.get(key);
            break;
        }
        return nextNode;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public void deRegister(HashableNode dataNode) {
        //注销之前先找到后继（顺时针方向）节点
        HashableNode nextNode = getNextNodeGreaterThan(dataNode);

        //将被注销的节点dataNode的数据导给下一个节点
        dataNode.exportDataTo(nextNode);

        //将自己从注册表中删除
        registry.remove(dataNode.getKey());
    }

    @Override
    public void put(Hashable data) {
        HashableNode targetNode = getTargetNode(data);
        if(order == 0) {
            targetNode.put(data);
        } else {
            //此时为带有限负载的一致性哈希
            LoadLimitedHashableNode loadLimitedNode = (LoadLimitedHashableNode)targetNode;
            if (loadLimitedNode.canTakeOne()) {
                loadLimitedNode.put(data);
            } else {
                LoadLimitedHashableNode nextNode =
                        (LoadLimitedHashableNode)getNextNodeGreaterThan(loadLimitedNode);
                int i = 0;
                while(!nextNode.canTakeOne() && i < order) {
                    nextNode = (LoadLimitedHashableNode)getNextNodeGreaterThan(nextNode);
                    i++;
                }
                if(i < order)
                    nextNode.put(data);
                else
                    throw new TooBusyException("经过" + i + "次寻找后，无法找到更多的空间！");
            }
        }
    }

    @Override
    public Hashable get(int key) {
        Hashable result = null;

        //无论是否带有限负载，直接在最近的节点上寻找数据
        HashableNode currNode = getNextNodeEqualOrGreaterThan(key);
        result = currNode.get(key);

        if (result == null && order > 0){
            //否则最多在哈希环顺时针方向上order个节点上寻找数据
            int i = 0;
            while (i < order) {
                currNode = getNextNodeGreaterThan(currNode);
                result = currNode.get(key);
                if (result != null) {
                    break;
                }
                i ++;
            }
        }

        return result;
    }

    @Override
    public HashableNode getTargetNode(Hashable data) {
        Integer key = data.getKey();

        return getNextNodeEqualOrGreaterThan(key);
    }
}
