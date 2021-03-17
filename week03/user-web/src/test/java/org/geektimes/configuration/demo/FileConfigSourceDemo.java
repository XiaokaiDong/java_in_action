package org.geektimes.configuration.demo;

import org.geektimes.config.source.FileConfigSource;

public class FileConfigSourceDemo {
    public static void main(String[] args) {
        FileConfigSource configSource = new FileConfigSource("/MyConfig.properties");

        for (String envVar: configSource.getPropertyNames()) {
            System.out.println(envVar + ": " + configSource.getValue(envVar));
        }


    }
}
