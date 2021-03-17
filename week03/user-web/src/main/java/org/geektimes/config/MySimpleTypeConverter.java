package org.geektimes.config;

import org.eclipse.microprofile.config.spi.Converter;
import org.geektimes.function.ThrowableFunction;
import org.geektimes.config.MyTypeDescriptor;


import java.util.HashMap;
import java.util.Map;

/**
 * 类型转换类，将String转换为基本类型的包装类
 * @param <T> 目标类型
 */
public class MySimpleTypeConverter<T> implements Converter<T> {

    private Map<Class<?>, ThrowableFunction<String, Object>> converterMap = new HashMap<>();

    /**
     * 将基本包装类型的工厂方法放入一个Map
     * 支持String到如下类型的转换
     * Boolean,
     * Byte,
     * Double,
     * Float,
     * Integer,
     * Long,
     * Short
     */
    public MySimpleTypeConverter() {
        converterMap.putIfAbsent(Boolean.class, Boolean::parseBoolean);
        converterMap.putIfAbsent(Byte.class, Byte::parseByte);
        converterMap.putIfAbsent(Double.class, Double::valueOf);
        converterMap.putIfAbsent(Float.class, Float::valueOf);
        converterMap.putIfAbsent(Integer.class, Integer::valueOf);
        converterMap.putIfAbsent(Long.class, Long::valueOf);
        converterMap.putIfAbsent(Short.class, Short::valueOf);
    }


    @Override
    public T convert(String value) throws IllegalArgumentException, NullPointerException {
        GenericUtil<T> genericUtil = new GenericUtil<T>(){};
        return convert(value, genericUtil.getType());
    }

    public T convert(String value, Class<T> targetType) throws IllegalArgumentException, NullPointerException {
        return (T)convertHelper(value, MyTypeDescriptor.valueOf(targetType));
    }

    public Object convertHelper(String value, MyTypeDescriptor targetType) {
        Object result = null;
        ThrowableFunction<String, Object> converter = converterMap.get(targetType.getType());
        if (converter == null) {
            throw new IllegalArgumentException("不支持的额类型");
        }
        try {
            result =  converter.apply(value);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        return result;
    }
}
