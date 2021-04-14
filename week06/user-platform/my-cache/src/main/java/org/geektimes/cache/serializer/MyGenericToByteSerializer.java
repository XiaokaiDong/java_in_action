package org.geektimes.cache.serializer;

import org.eclipse.microprofile.config.spi.Converter;
import org.geektimes.cache.util.Assert;
import org.geektimes.configuration.microprofile.config.converter.Converters;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * java基本类型的序列化和反序列化
 */
public class MyGenericToByteSerializer<T> implements MySerializer<T>{

    private final Converters converters;
    private final Charset charset;

    private final Class<T> type;

    public MyGenericToByteSerializer(Charset charset, Class<T> type) {
        Assert.notNull(type, "Type must not be null!");

        this.charset = charset;
        this.type = type;
        this.converters = new Converters();
        converters.addDiscoveredConverters();
    }

    public MyGenericToByteSerializer(Class<T> type) {
        this(StandardCharsets.UTF_8, type);
    }

    /**
     * 利用了org.geektimes.configuration.microprofile.config.converter.Converters
     * @param t
     * @return
     * @throws Exception
     */
    @Override
    public byte[] serialize(T t) throws Exception {
        String result = String.valueOf(t);
        return result.getBytes(charset);
    }

    @Override
    public T deserialize(byte[] bytes) throws Exception {
        if (bytes == null) {
            return null;
        }

        String string = new String(bytes, charset);
        List<Converter> converterList = converters.getConverters(type);
        Converter converter = null;
        if (converterList.size() > 0) {
            converter = converterList.get(0);
            return (T)converter.convert(string);
        }else{
            return null;
        }

    }


}
