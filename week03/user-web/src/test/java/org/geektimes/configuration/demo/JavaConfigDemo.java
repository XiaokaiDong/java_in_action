package org.geektimes.configuration.demo;

import org.geektimes.config.JavaConfig;

public class JavaConfigDemo {
    public static void main(String[] args) {
        JavaConfig config = new JavaConfig();
        System.out.println(config.getValue("application.name", String.class));

        //int id = config.getValue("id", Integer.class);
    }
}
