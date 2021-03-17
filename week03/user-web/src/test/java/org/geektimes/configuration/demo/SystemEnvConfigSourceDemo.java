package org.geektimes.configuration.demo;

import org.geektimes.config.source.SystemEnvConfigSource;

public class SystemEnvConfigSourceDemo {
    public static void main(String[] args) {
        SystemEnvConfigSource configSource = new SystemEnvConfigSource();

        for (String envVar: configSource.getPropertyNames()) {
            System.out.println(envVar + ": " + configSource.getValue(envVar));
        }


    }
}
