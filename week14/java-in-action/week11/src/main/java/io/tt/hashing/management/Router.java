package io.tt.hashing.management;

import io.tt.hashing.entity.Hashable;
import io.tt.hashing.entity.HashableNode;

/**
 * 数据路由器，给出一个{@link Hashable}时，得到该对象应该被存储的节点
 */
public interface Router {

    /**
     * 根据数据找到对应的存储节点
     * @param data 需要被存储的数据
     * @return 该数据应该被存储的节点
     */
    HashableNode getTargetNode(Hashable data);
}
