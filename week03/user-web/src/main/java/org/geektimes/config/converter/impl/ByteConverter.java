package org.geektimes.config.converter.impl;

import org.geektimes.config.converter.MyAbstractTypeConverter;
import org.geektimes.function.ThrowableFunction;

public class ByteConverter extends MyAbstractTypeConverter<Byte> {

    @Override
    protected Byte doConvert(String value) {
        ThrowableFunction<String, Object> converter = getConvertingFunc(resolveConvertedType(this));
        try {
            return (Byte) converter.apply(value);
        } catch (Throwable throwable) {
            throw new IllegalArgumentException(throwable.getMessage());
        }
    }
}
