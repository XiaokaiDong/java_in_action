package io.tt.hashing.exception;

/**
 * 不兼容的数据类型异常
 */
public class IncompatibleTypeException extends RuntimeException{
    public IncompatibleTypeException(String message) {
        super(message);
    }
}
