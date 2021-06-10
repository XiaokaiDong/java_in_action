package io.tt.hashing.entity;

/**
 * 可哈希的对象
 * 可以用来表示数据，也可以用来表示节点
 */
public interface Hashable {
    /**
     * 可哈希的对象都可以返回一个整型的键值
     * @return
     */
    int getKey();
}
