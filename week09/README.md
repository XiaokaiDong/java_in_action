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

  根据上述的分析，需要将@Cacheable注解中的属性取出来用于RedisCacheManager加载缓存，那么，可以让RedisCacheManager实现BeanPostProcessor#postProcessBeforeInitialization，在这个方法中使用SpringCacheAnnotationParser解析@Cacheable中的信息，然后使用这些信息加载Redis缓存。

- 具体实现

  - RedisCacheManager在实现BeanPostProcessor#postProcessBeforeInitialization时，可以直接new一个AnnotationCacheOperationSource实例，在构造这个实例的时候，直接new一个SpringCacheAnnotationParser对象作为构造函数的参数

  - 调用AnnotationCacheOperationSource#findCacheOperations(java.lang.reflect.Method)得到Collection\<CacheOperation\>，然后将这些属性用于构造RedisCacheManager。