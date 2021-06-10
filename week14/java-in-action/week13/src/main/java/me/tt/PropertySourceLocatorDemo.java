package me.tt;

import me.tt.locator.MyPropertySourceLocator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class PropertySourceLocatorDemo implements EnvironmentAware {

    private Environment environment;

    @Value("${config.file}")
    private String configFile;

    public static void main(String[] args) {
        SpringApplication.run(PropertySourceLocatorDemo.class, args);
    }

    @Bean
    ApplicationRunner runner(){
        return args -> System.out.printf("configFile = %s\n", environment.resolvePlaceholders("${config.file}"));
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Bean
    MyPropertySourceLocator myPropertySourceLocator(){
        return new MyPropertySourceLocator();
    }
}
