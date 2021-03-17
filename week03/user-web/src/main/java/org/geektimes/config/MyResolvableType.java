package org.geektimes.config;

import java.lang.reflect.Type;

public class MyResolvableType {

    private Class<?> resolved;

    private final Type type;

    public static MyResolvableType forClass( Class<?> clazz) {
        return new MyResolvableType(clazz);
    }

    private MyResolvableType( Class<?> clazz) {
        this.resolved = (clazz != null ? clazz : Object.class);
        this.type = this.resolved;
    }

    public Class<?> toClass() {
        return resolve(Object.class);
    }

    public Class<?> resolve(Class<?> fallback) {
        return (this.resolved != null ? this.resolved : fallback);
    }
}
