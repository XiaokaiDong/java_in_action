package org.geektimes.config.converter;

import org.eclipse.microprofile.config.spi.Converter;
import org.geektimes.function.ThrowableFunction;


import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * 类型转换类，将String转换为基本类型的包装类
 * @param <T> 目标类型
 */
public abstract class MyAbstractTypeConverter<T> implements Converter<T> {

    private final static Map<Class<?>, ThrowableFunction<String, Object>> converterMap = new HashMap<>();

    static {
        converterMap.putIfAbsent(Boolean.class, Boolean::parseBoolean);
        converterMap.putIfAbsent(Byte.class, Byte::parseByte);
        converterMap.putIfAbsent(Double.class, Double::valueOf);
        converterMap.putIfAbsent(Float.class, Float::valueOf);
        converterMap.putIfAbsent(Integer.class, Integer::valueOf);
        converterMap.putIfAbsent(Long.class, Long::valueOf);
        converterMap.putIfAbsent(Short.class, Short::valueOf);
    }

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
    public MyAbstractTypeConverter() {

    }


    @Override
    public T convert(String value) throws IllegalArgumentException, NullPointerException {
        if (value == null) {
            throw new NullPointerException("The value must not be null!");
        }
        return doConvert(value);
    }

    protected abstract T doConvert(String value);

    private void assertConverter(Converter<?> converter) {
        Class<?> converterClass = converter.getClass();
        if (converterClass.isInterface()) {
            throw new IllegalArgumentException("The implementation class of Converter must not be an interface!");
        }
        if (Modifier.isAbstract(converterClass.getModifiers())) {
            throw new IllegalArgumentException("The implementation class of Converter must not be abstract!");
        }
    }

    private Class<?> resolveConvertedType(Type type) {
        Class<?> convertedType = null;
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            if (pType.getRawType() instanceof Class) {
                Class<?> rawType = (Class) pType.getRawType();
                if (Converter.class.isAssignableFrom(rawType)) {
                    Type[] arguments = pType.getActualTypeArguments();
                    if (arguments.length == 1 && arguments[0] instanceof Class) {
                        convertedType = (Class) arguments[0];
                    }
                }
            }
        }
        return convertedType;
    }

    private Class<?> resolveConvertedType(Class<?> converterClass) {
        Class<?> convertedType = null;

        for (Type superInterface : converterClass.getGenericInterfaces()) {
            convertedType = resolveConvertedType(superInterface);
            if (convertedType != null) {
                break;
            }
        }

        return convertedType;
    }

    public Class<?> resolveConvertedType(Converter<?> converter) {
        assertConverter(converter);
        Class<?> convertedType = null;
        Class<?> converterClass = converter.getClass();
        while (converterClass != null) {
            convertedType = resolveConvertedType(converterClass);
            if (convertedType != null) {
                break;
            }

            Type superType = converterClass.getGenericSuperclass();
            if (superType instanceof ParameterizedType) {
                convertedType = resolveConvertedType(superType);
            }

            if (convertedType != null) {
                break;
            }
            // recursively
            converterClass = converterClass.getSuperclass();

        }

        return convertedType;
    }

    protected ThrowableFunction<String, Object> getConvertingFunc(Class<?> targetClass) {
        if (converterMap.containsKey(targetClass))
            return converterMap.get(targetClass);
        else
            return null;
    }
}
