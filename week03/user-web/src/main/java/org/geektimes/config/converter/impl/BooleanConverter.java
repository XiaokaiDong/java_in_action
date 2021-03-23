package org.geektimes.config.converter.impl;

import org.geektimes.config.converter.MyAbstractTypeConverter;
import org.geektimes.function.ThrowableFunction;

public class BooleanConverter extends MyAbstractTypeConverter<Boolean> {

    @Override
    protected Boolean doConvert(String value) {
        ThrowableFunction<String, Object> converter = getConvertingFunc(resolveConvertedType(this));
        try {
            return (Boolean) converter.apply(value);
        } catch (Throwable throwable) {
            throw new IllegalArgumentException(throwable.getMessage());
        }
    }
}
