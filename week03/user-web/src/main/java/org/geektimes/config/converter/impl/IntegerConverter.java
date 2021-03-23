package org.geektimes.config.converter.impl;

import org.geektimes.config.converter.MyAbstractTypeConverter;
import org.geektimes.function.ThrowableFunction;

public class IntegerConverter extends MyAbstractTypeConverter<Integer> {

    @Override
    protected Integer doConvert(String value) {
        ThrowableFunction<String, Object> converter = getConvertingFunc(resolveConvertedType(this));
        try {
            return (Integer) converter.apply(value);
        } catch (Throwable throwable) {
            throw new IllegalArgumentException(throwable.getMessage());
        }
    }
}
