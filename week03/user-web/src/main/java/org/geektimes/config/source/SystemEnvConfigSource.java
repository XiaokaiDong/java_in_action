package org.geektimes.config.source;

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.Map;
import java.util.Set;

public class SystemEnvConfigSource implements ConfigSource {
    private final Map<String, String> properties;

    public SystemEnvConfigSource() {
        this.properties = System.getenv();
    }

    @Override
    public Set<String> getPropertyNames() {

        return properties.keySet();
    }

    @Override
    public String getValue(String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public String getName() {
        return "Host System Environment";
    }
}
