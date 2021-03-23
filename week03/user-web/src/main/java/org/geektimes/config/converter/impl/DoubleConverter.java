package org.geektimes.config.converter.impl;

import org.geektimes.config.converter.MyAbstractTypeConverter;
import org.geektimes.function.ThrowableFunction;

public class DoubleConverter extends MyAbstractTypeConverter<Double> {

    @Override
    protected Double doConvert(String value) {
        ThrowableFunction<String, Object> converter = getConvertingFunc(resolveConvertedType(this));
        try {
            return (Double) converter.apply(value);
        } catch (Throwable throwable) {
            throw new IllegalArgumentException(throwable.getMessage());
        }
    }
}
