package io.tt.service;

import io.tt.cache.CacheManagerResolver;
import io.tt.cache.registry.CacheOperationRegistry;
import io.tt.model.Coffee;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@CacheConfig(cacheNames = "coffee")
public class CoffeeService implements SmartInitializingSingleton {

    @Autowired
    CacheManagerResolver cacheManagerResolver;

    @Autowired
    CacheManager cacheManager;

    @Cacheable
    public List<Coffee> findAllCoffee() {
        Coffee coffee = new Coffee();
        coffee.setName("Mocha");
        return Collections.singletonList(coffee);
    }

    @Override
    public void afterSingletonsInstantiated() {
        if(cacheManagerResolver.isCandidateClass(this.getClass())){
            for (Method method: this.getClass().getMethods()) {
                cacheManagerResolver.addCacheOperations(
                        cacheManagerResolver.getCacheOperations(method, this.getClass())
                );
            }

        }
    }
}


