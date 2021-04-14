package org.geektimes.cache.serializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.geektimes.cache.util.Assert;


/**
 * 利用jackson进行JSON序列化
 */
public class MyGenericJackson2JsonSerializer implements MySerializer<Object>{

    private final ObjectMapper mapper;

    public MyGenericJackson2JsonSerializer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public MyGenericJackson2JsonSerializer() {
        this.mapper = new ObjectMapper();
        //序列化时包括所有属性
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        //遇到未知属性不报错
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //空对象抛异常
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);
        //忽略transient修饰的属性
        mapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true);

    }

    @Override
    public byte[] serialize(Object o) throws Exception {
        return new byte[0];
    }

    @Override
    public Object deserialize(byte[] bytes) throws Exception {

        return deserialize(bytes, Object.class);
    }

    public <T> T deserialize(byte[] source, Class<T> type) throws Exception {
        Assert.notNull(type, "Type must not be null!");
        return mapper.readValue(source, type);
    }
}
