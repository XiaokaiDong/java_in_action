package io.tt.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan(basePackages = "io.tt")
@Configuration
public class AnnotationRedisCacheDemo {
    @Autowired
    private CacheManagerResolver cacheManagerResolver;

    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

        applicationContext.register(AnnotationRedisCacheDemo.class);

        // 启动 Spring 应用上下文
        applicationContext.refresh();

        AnnotationRedisCacheDemo annotationRedisCacheDemo =
                (AnnotationRedisCacheDemo) applicationContext.getBean("annotationRedisCacheDemo");

        RedisCacheManager cacheManager =
                (RedisCacheManager)annotationRedisCacheDemo.cacheManagerResolver.getCacheManager();

        System.out.println(cacheManager.getCacheOperations());

        // 显示地关闭 Spring 应用上下文
        applicationContext.close();
    }

    @Bean
    CacheManagerResolver cacheManagerResolver(){
        return new CacheManagerResolver();
    }
}
