package org.geektimes.config;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class GenericUtil<T> {
    private Class<T> tClass;

    GenericUtil() {

        Type type = getClass().getGenericSuperclass();
        Type trueType = ((ParameterizedType) type).getActualTypeArguments()[0];
        //下面这句话会抛异常，不知道为啥
        this.tClass = (Class<T>) trueType;
    }

    Class<T> getType(){
        return tClass;
    }
}
