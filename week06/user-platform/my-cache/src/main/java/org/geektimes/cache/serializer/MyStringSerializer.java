package org.geektimes.cache.serializer;

import org.geektimes.cache.util.Assert;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * String类型的序列化
 */
public class MyStringSerializer implements MySerializer<String>{
    private final Charset charset;

    public MyStringSerializer(Charset charset) {
        Assert.notNull(charset, "Charset must not be null!");
        this.charset = charset;
    }

    public MyStringSerializer() {
        this(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] serialize(String s) throws Exception {
        return (s == null ? null : s.getBytes(charset));
    }

    @Override
    public String deserialize(byte[] bytes) throws Exception {
        return (bytes == null ? null : new String(bytes, charset));
    }


}
