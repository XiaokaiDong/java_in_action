# 小马哥JAVA实战营第九周作业


## 作业内容1


> - 如何清除某个 Spring Cache 所有的 Keys 关联的对象
>   - 如果 Redis 中心化方案，Redis + Sentinel
>   - 如果 Redis 去中心化方案，Redis Cluster

### 解答

这个问题的关键是将Cache中的Key按照名称空间的形式来组织，使得Key之间的关系是树形的。

对于Redis来说，为了实现从键到值的快速访问，Redis 使用了一个哈希表来保存所有键值对。一个哈希表，其实就是一个数组，数组的每个元素称为一个哈希桶。所以，我们常说，一个哈希表是由多个哈希桶组成的，每个哈希桶中保存了键值对数据。

REDIS中，所有的数据都有一个键，这个键是全局哈希表的一个“索引”。但值类型是HASH的时候，就会有两个键：一个是全局哈希表中的键，一个是HASH这个值中的键。

这样，上述的两个键天然就是有层次关系的。所以如果Spring Cache 所有的 Keys都是底层Redis某个Hash数据结构中的键时，只要清空全局哈希表中这个Hash数据结构对应的键，就可以清空这个Spring Cache 所有的 Keys了。

## 作业内容2

> 如何将 RedisCacheManager 与 @Cacheable 注解打通?

### 解答

这个问题的本质是将@Cacheable注解中的属性取出来，用于RedisCacheManager中重载的RedisCacheManager#loadCaches -> RedisCacheManager#prepareCaches方法中加载缓存。

缓存操作元数据来源是CacheOperationSource，它有一个注解实现AnnotationCacheOperationSource，具体的解析过程是依靠缓存注解解析器CacheAnnotationParser，具体实现是SpringCacheAnnotationParser。

- 整体思路

  根据上述的分析，需要将@Cacheable注解中的属性取出来用于RedisCacheManager加载缓存，那么，可以在初始化相应Bean的时候，扫描自己，发现所有的@Cacheable注解，取出其中的信息，用于构建RedisCacheManager。

- 具体实现

  - 实现CacheManager工厂类 CacheManagerResolver，将操作委派给AnnotationCacheOperationSource。

    ```java
    /**
    * CacheManager工厂类
    */
    public class CacheManagerResolver implements CacheOperationSource, CacheOperationRegistry {

        private final CacheOperationSource cacheOperationSource =
                new AnnotationCacheOperationSource(new SpringCacheAnnotationParser());


        @Override
        public boolean isCandidateClass(Class<?> targetClass) {
            return cacheOperationSource.isCandidateClass(targetClass);
        }

        @Override
        public Collection<CacheOperation> getCacheOperations(Method method, Class<?> targetClass) {
            return cacheOperationSource.getCacheOperations(method, targetClass);
        }

        public CacheManager getCacheManager(){
            RedisCacheManager redisCacheManager = new RedisCacheManager("");
            redisCacheManager.setCacheOperations(getCacheOperations());
            return redisCacheManager;
        }

        @Override
        public void addCacheOperations(Collection<CacheOperation> cacheOperations) {
            //TODO
        }

        @Override
        public Collection<CacheOperation> getCacheOperations() {
            //TODO
            return null;
        }
    }
    ```

  - 其中CacheOperationRegistry定义如下

    ```java
    /**
    * 管理所有的CacheOperation
    */
    public interface CacheOperationRegistry {

        /**
        * 添加 CacheOperation。需要做到去重
        * @param cacheOperations 需要被添加的CacheOperation
        */
        void addCacheOperations(Collection<CacheOperation> cacheOperations);

        /**
        * 返回所有的CacheOperation对象
        * @return CacheOperation对象
        */
        Collection<CacheOperation> getCacheOperations();
    }
    ```

  - 在定义服务类的时候，让其实现SmartInitializingSingleton接口，这样初始化Bean的时候，应用上下文就可以调用它来扫描缓存信息了.

    比如CoffeeService在afterSingletonsInstantiated方法实现中获取缓存信息。

    ```java
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
    ```

  - 修改RedisCacheManager，使其可以保存所有的CacheOperation信息，并更具这些信息来创建缓存（尚未实现）

    ```java
    private Collection<CacheOperation> cacheOperations;

    public void setCacheOperations(Collection<CacheOperation> cacheOperations) {
        this.cacheOperations = cacheOperations;
    }
    ```