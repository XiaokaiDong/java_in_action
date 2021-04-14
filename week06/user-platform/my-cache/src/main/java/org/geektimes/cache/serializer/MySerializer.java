package org.geektimes.cache.serializer;

public interface MySerializer<T> {
    byte[] serialize(T t) throws Exception;

    T deserialize(byte[] bytes) throws Exception;
}
