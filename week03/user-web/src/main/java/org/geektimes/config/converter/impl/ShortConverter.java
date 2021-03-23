package org.geektimes.config.converter.impl;

import org.geektimes.config.converter.MyAbstractTypeConverter;
import org.geektimes.function.ThrowableFunction;

public class ShortConverter extends MyAbstractTypeConverter<Short> {

    @Override
    protected Short doConvert(String value) {
        ThrowableFunction<String, Object> converter = getConvertingFunc(resolveConvertedType(this));
        try {
            return (Short) converter.apply(value);
        } catch (Throwable throwable) {
            throw new IllegalArgumentException(throwable.getMessage());
        }
    }
}
