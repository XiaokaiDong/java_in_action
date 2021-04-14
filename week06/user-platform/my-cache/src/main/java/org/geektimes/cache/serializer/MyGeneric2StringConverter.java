package org.geektimes.cache.serializer;

import org.eclipse.microprofile.config.spi.Converter;
import org.geektimes.configuration.microprofile.config.converter.Converters;

import java.util.List;

/**
 * 基本类型和字符串类型的互相转换
 * @param <T>
 */
public class MyGeneric2StringConverter<T> {
    private final Converters converters;

    private final Class<T> type;

    public MyGeneric2StringConverter(Class<T> type) {
        this.converters = new Converters();
        converters.addDiscoveredConverters();
        this.type = type;
    }

    public String of(T t) throws Exception {
        return String.valueOf(t);
    }

    public T from(String string) throws Exception {
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
