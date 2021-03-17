package org.geektimes.config;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public  class MyTypeDescriptor implements Serializable {

    private static final Map<Class<?>, MyTypeDescriptor> commonTypesCache = new HashMap<>(32);

    private static final Class<?>[] CACHED_COMMON_TYPES = {
            boolean.class, Boolean.class, byte.class, Byte.class, char.class, Character.class,
            double.class, Double.class, float.class, Float.class, int.class, Integer.class,
            long.class, Long.class, short.class, Short.class, String.class, Object.class};

    static {
        for (Class<?> preCachedClass : CACHED_COMMON_TYPES) {
            commonTypesCache.put(preCachedClass, valueOf(preCachedClass));
        }
    }

    private Class<?> type;
    private final MyResolvableType resolvableType;

//    public MyTypeDescriptor(Class<?> type) {
//        this.type = type;
//    }

    public MyTypeDescriptor(MyResolvableType resolvableType) {
        this.resolvableType = resolvableType;
        this.type = (type != null ? type : resolvableType.toClass());
    }


    public Class getType() {
        return type;
    }

    public static MyTypeDescriptor valueOf( Class<?> type) {
        if (type == null) {
            type = Object.class;
        }
        MyTypeDescriptor desc = commonTypesCache.get(type);
        return (desc != null ? desc : new MyTypeDescriptor(MyResolvableType.forClass(type)));
    }
}
