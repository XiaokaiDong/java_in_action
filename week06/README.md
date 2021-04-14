# 小马哥JAVA实战营第六周作业


## 作业内容1


> 提供一套抽象 API 实现对象的序列化和反序列化


### 新建包org.geektimes.cache.serializer，并建立接口类org.geektimes.cache.serializer.MySerializer

  ```java
  public interface MySerializer<T> {
    byte[] serialize(T t) throws Exception;

    T deserialize(byte[] bytes) throws Exception;
  }
  ```

### 创建几个实现类

- org.geektimes.cache.serializer.MyStringSerializer用来序列化字符串类型

  ```java
  /**
   * String类型的序列化
   */
  public class MyStringSerializer implements MySerializer<String>{
    private final Charset charset;

    public MyStringSerializer(Charset charset) {
        Assert.notNull(charset, "Charset must not be null!");
        this.charset = charset;
    }

    public MyStringSerializer() {
        this(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] serialize(String s) throws Exception {
        return (s == null ? null : s.getBytes(charset));
    }

    @Override
    public String deserialize(byte[] bytes) throws Exception {
        return (bytes == null ? null : new String(bytes, charset));
    }


  }
  ```

- org.geektimes.cache.serializer.MyGenericToByteSerializer用来序列化基本类型
  
  利用了已有的org.geektimes.configuration.microprofile.config.converter.Converters

  ```java
    /**
     * java基本类型的序列化和反序列化
     */
    public class MyGenericToByteSerializer<T> implements MySerializer<T>{

        private final Converters converters;
        private final Charset charset;

        private final Class<T> type;

        public MyGenericToByteSerializer(Charset charset, Class<T> type) {
            Assert.notNull(type, "Type must not be null!");

            this.charset = charset;
            this.type = type;
            this.converters = new Converters();
            converters.addDiscoveredConverters();
        }

        public MyGenericToByteSerializer(Class<T> type) {
            this(StandardCharsets.UTF_8, type);
        }

        /**
        * 利用了org.geektimes.configuration.microprofile.config.converter.Converters
        * @param t
        * @return
        * @throws Exception
        */
        @Override
        public byte[] serialize(T t) throws Exception {
            String result = String.valueOf(t);
            return result.getBytes(charset);
        }

        @Override
        public T deserialize(byte[] bytes) throws Exception {
            if (bytes == null) {
                return null;
            }

            String string = new String(bytes, charset);
            List<Converter> converterList = converters.getConverters(type);
            Converter converter = null;
            if (converterList.size() > 0) {
                converter = converterList.get(0);
                return (T)converter.convert(string);
            }else{
                return null;
            }

        }
    }
  ```

- org.geektimes.cache.serializer.MyGenericJackson2JsonSerializer用来序列化对象到JSON，利用了com.fasterxml.jackson.databind.ObjectMapper

  ```java
    /**
    * 利用jackson进行JSON序列化
    */
    public class MyGenericJackson2JsonSerializer implements MySerializer<Object>{

        private final ObjectMapper mapper;

        public MyGenericJackson2JsonSerializer(ObjectMapper mapper) {
            this.mapper = mapper;
        }

        public MyGenericJackson2JsonSerializer() {
            this.mapper = new ObjectMapper();
            //序列化时包括所有属性
            mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
            //遇到未知属性不报错
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            //空对象抛异常
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);
            //忽略transient修饰的属性
            mapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true);

        }

        @Override
        public byte[] serialize(Object o) throws Exception {
            return new byte[0];
        }

        @Override
        public Object deserialize(byte[] bytes) throws Exception {

            return deserialize(bytes, Object.class);
        }

        public <T> T deserialize(byte[] source, Class<T> type) throws Exception {
            Assert.notNull(type, "Type must not be null!");
            return mapper.readValue(source, type);
        }
    }
  ```

### 额外的，为了实现LettuceCache方便（用了RedisCommands<String, String>），建立类org.geektimes.cache.serializer.MyGeneric2StringConverter

也利用了已有的org.geektimes.configuration.microprofile.config.converter.Converters。

```java
/**
 * 基本类型和字符串类型的互相转换
 * @param <T>
 */
public class MyGeneric2StringConverter<T> {
    private final Converters converters;

    private final Class<T> type;

    public MyGeneric2StringConverter(Class<T> type) {
        this.converters = new Converters();
        converters.addDiscoveredConverters();
        this.type = type;
    }

    public String of(T t) throws Exception {
        return String.valueOf(t);
    }

    public T from(String string) throws Exception {
        List<Converter> converterList = converters.getConverters(type);
        Converter converter = null;
        if (converterList.size() > 0) {
            converter = converterList.get(0);
            return (T)converter.convert(string);
        }else{
            return null;
        }
    }
}
```

## 作业内容2


> 通过 Lettuce 实现一套 Redis CacheManager 以及 Cache

### LettuceCacheManager

主要是使用了io.lettuce.core.RedisClient，将StatefulRedisConnection对象作为参数实例化LettuceCache。

```java
public class LettuceCacheManager extends AbstractCacheManager {

    private RedisClient client;
    private StatefulRedisConnection<String, String> connection;

    public LettuceCacheManager(CachingProvider cachingProvider, URI uri, ClassLoader classLoader, Properties properties) {
        super(cachingProvider, uri, classLoader, properties);
        client = RedisClient.create(RedisURI.create(uri));
        connection = client.connect();
    }

    @Override
    protected <K, V, C extends Configuration<K, V>> Cache doCreateCache(String cacheName, C configuration) {
        return new LettuceCache(this, cacheName, configuration, connection);
    }

    @Override
    protected void doClose() {
        connection.close();
        client.shutdown();
    }
}
```

### LettuceCache

- 分别设置键和值的序列化器。利用了第一个作业创建的org.geektimes.cache.serializer.MyGeneric2StringConverter进行键序列化，用org.geektimes.cache.serializer.MyGenericJackson2JsonSerializer进行值序列化。

```java
private final Logger logger = Logger.getLogger(LettuceCache.class.getName());

private final StatefulRedisConnection connection;

private final RedisCommands<String, String> redisCommands;

private final MyGeneric2StringConverter<K> keySerializer;

private final MySerializer<Object> valueSerializer = new MyGenericJackson2JsonSerializer();
```

- 在构造函数中初始化键序列化器

  ```java
    protected LettuceCache(CacheManager cacheManager, String cacheName,
                           Configuration<K, V> configuration, StatefulRedisConnection connection) {
        super(cacheManager, cacheName, configuration);
        this.connection = connection;
        this.redisCommands = connection.sync();
        this.keySerializer = new MyGeneric2StringConverter<>(configuration.getKeyType());
    }
  ```

- 剩余实现多次利用了键/值的序列化器

  ```java
    @Override
    protected boolean containsEntry(K key) throws CacheException, ClassCastException {
        try {
            return redisCommands.exists(keySerializer.of(key)) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected ExpirableEntry<K, V> getEntry(K key) throws CacheException, ClassCastException {
        try {
            return getEntry(keySerializer.of(key));
        } catch (Exception e) {
            return null;
        }
    }

    protected ExpirableEntry<K, V> getEntry(String keyString) throws CacheException, ClassCastException {
        String stringValue = redisCommands.get(keyString);

        try {
            return ExpirableEntry.of(keySerializer.from(keyString),
                    (V)valueSerializer.deserialize(stringValue.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void putEntry(ExpirableEntry<K, V> entry) throws CacheException, ClassCastException {
        try {
            redisCommands.set(keySerializer.of(entry.getKey()),
                    new String(valueSerializer.serialize(entry.getValue()), StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.log(Level.WARNING, "序列化错误" + e.getMessage());
        }

    }

    @Override
    protected ExpirableEntry<K, V> removeEntry(K key) throws CacheException, ClassCastException {
        ExpirableEntry<K, V> oldEntry = getEntry(key);
        try {
            redisCommands.del(keySerializer.of(key));
        } catch (Exception e) {
            logger.log(Level.WARNING, "删除键错误" + e.getMessage());
        }

        return oldEntry;
    }

    @Override
    protected void clearEntries() throws CacheException {
        redisCommands.flushall();
    }

    @Override
    protected Set<K> keySet() {
        return null;
    }
  ```