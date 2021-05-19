package io.tt.hashing.exception;

/**
 * 当一个节点压力比较大时，抛出此异常，表示应该将数据放入下一个节点
 * 用于实现"带有限负载的一致性哈希"
 */
public class TooBusyException extends RuntimeException{

    public TooBusyException(String message) {
        super(message);
    }
}
