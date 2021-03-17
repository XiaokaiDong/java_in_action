# 小马哥JAVA实战营第三周作业

基于小马哥的课程案例

## 作业内容1


>整合 https://jolokia.org/
>  实现一个自定义 JMX MBean，通过 Jolokia 做 Servlet 代理


- 添加MAVEN依赖

  ```
  <dependency>
      <groupId>org.jolokia</groupId>
      <artifactId>jolokia-core</artifactId>
      <version>${jolokia.version}</version>
  </dependency>
  ```

- 增加web.xml配置

  ```
  <servlet>
      <servlet-name>jolokia-agent</servlet-name>
      <servlet-class>org.jolokia.http.AgentServlet</servlet-class>
      <load-on-startup>1</load-on-startup>
  </servlet>

  <servlet-mapping>
      <servlet-name>jolokia-agent</servlet-name>
      <url-pattern>/jolokia/*</url-pattern>
  </servlet-mapping>
  ```

  然后在ComponentContextInitializerListener中初始化一个UserManager就可以通过http访问了
  ```java
    //加载MBean
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    ObjectName objectName = null;
    try {
        objectName = new ObjectName("org.geektimes.projects.user.management:type=User");
        User user = new User();
        mBeanServer.registerMBean(createUserMBean(user), objectName);
    } catch (Exception e){
        e.printStackTrace();
    }
  ```

## 作业内容2


>扩展 org.eclipse.microprofile.config.spi.ConfigSource 实现，包括 OS 环境变量，以及本地配置文件


- OS 环境变量ConfigSource

  ```java
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
  ```

  测试
  ```java
  public class SystemEnvConfigSourceDemo {
    public static void main(String[] args) {
        SystemEnvConfigSource configSource = new SystemEnvConfigSource();

        for (String envVar: configSource.getPropertyNames()) {
            System.out.println(envVar + ": " + configSource.getValue(envVar));
        }


    }
  }
  ```

- 本地配置文件ConfigSource

  ```java
  public class FileConfigSource implements ConfigSource {

    private final Logger logger = Logger.getLogger(FileConfigSource.class.getName());

    private final Map<String, String> properties;

    private final static String FILE_CHARSET = "GBK";

    public FileConfigSource() {
        this("MyConfig.properties");
    }

    public FileConfigSource(String proFileName) {
        Map<String, String> properties1;
        Properties fileProperties = new Properties();
        //InputStream input = Object.class.getResourceAsStream(proFileName);
        InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(proFileName);
        InputStreamReader inputStreamReader = null;

        properties1 = null;

        if (input != null) {
            try {
                inputStreamReader = new InputStreamReader(input, FILE_CHARSET);
                fileProperties.load(inputStreamReader);
                properties1 = new HashMap<String, String>((Map)fileProperties);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "加载配置文件" + proFileName + "失败");
            }
        }else {
            properties1 = new HashMap<>();
        }
        this.properties = properties1;
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
        return "Config File Properties";
    }
  }  
  ```

  测试
  ```java
  public class FileConfigSourceDemo {
    public static void main(String[] args) {
        FileConfigSource configSource = new FileConfigSource("/MyConfig.properties");

        for (String envVar: configSource.getPropertyNames()) {
            System.out.println(envVar + ": " + configSource.getValue(envVar));
        }


    }
  } 
  ```

##作业内容3

> 扩展 org.eclipse.microprofile.config.spi.Converter 实现，提供 String 类型到简单类型

- 借鉴了spring的类型转换

  使用MySimpleTypeConverter实现org.eclipse.microprofile.config.spi.Converter接口

  ```java
    /**
     * 类型转换类，将String转换为基本类型的包装类
    * @param <T> 目标类型
    */
    public class MySimpleTypeConverter<T> implements Converter<T> {

        private Map<Class<?>, ThrowableFunction<String, Object>> converterMap = new HashMap<>();

        /**
        * 将基本包装类型的工厂方法放入一个Map
        * 支持String到如下类型的转换
        * Boolean,
        * Byte,
        * Double,
        * Float,
        * Integer,
        * Long,
        * Short
        */
        public MySimpleTypeConverter() {
            converterMap.putIfAbsent(Boolean.class, Boolean::parseBoolean);
            converterMap.putIfAbsent(Byte.class, Byte::parseByte);
            converterMap.putIfAbsent(Double.class, Double::valueOf);
            converterMap.putIfAbsent(Float.class, Float::valueOf);
            converterMap.putIfAbsent(Integer.class, Integer::valueOf);
            converterMap.putIfAbsent(Long.class, Long::valueOf);
            converterMap.putIfAbsent(Short.class, Short::valueOf);
        }


        @Override
        public T convert(String value) throws IllegalArgumentException, NullPointerException {
            GenericUtil<T> genericUtil = new GenericUtil<T>(){};
            return convert(value, genericUtil.getType());
        }

        public T convert(String value, Class<T> targetType) throws IllegalArgumentException, NullPointerException {
            return (T)convertHelper(value, MyTypeDescriptor.valueOf(targetType));
        }

        public Object convertHelper(String value, MyTypeDescriptor targetType) {
            Object result = null;
            ThrowableFunction<String, Object> converter = converterMap.get(targetType.getType());
            if (converter == null) {
                throw new IllegalArgumentException("不支持的额类型");
            }
            try {
                result =  converter.apply(value);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }

            return result;
        }
    }
  ```

  - MySimpleTypeConverter用在JavaConfig#getConverter中

    ```java
    @Override
    public <T> Optional<Converter<T>> getConverter(Class<T> forType) {
        return Optional.of(new MySimpleTypeConverter<T>());
    }

    //提供类型转换
    @Override
    public <T> T getValue(String propertyName, Class<T> propertyType) {
        String propertyValue = getPropertyValue(propertyName);
        // String 转换成目标类型
        return getConverter(propertyType).get().convert(propertyValue);
    }
    ```

- 不得不说JAVA相比PYTHON实在是啰嗦，为了获取类型需要绕一大圈。

- 其中MyTypeDescriptor的定义如下，主要的作用是用来保存类型Class<?>

  ```java
  public  class MyTypeDescriptor implements Serializable {

    private static final Map<Class<?>, MyTypeDescriptor> commonTypesCache = new HashMap<>(32);

    private static final Class<?>[] CACHED_COMMON_TYPES = {
            boolean.class, Boolean.class, byte.class, Byte.class, char.class, Character.class,
            double.class, Double.class, float.class, Float.class, int.class, Integer.class,
            long.class, Long.class, short.class, Short.class, String.class, Object.class};

    static {
        for (Class<?> preCachedClass : CACHED_COMMON_TYPES) {
            commonTypesCache.put(preCachedClass, valueOf(preCachedClass));
        }
    }

    private Class<?> type;
    private final MyResolvableType resolvableType;


    public MyTypeDescriptor(MyResolvableType resolvableType) {
        this.resolvableType = resolvableType;
        this.type = (type != null ? type : resolvableType.toClass());
    }


    public Class getType() {
        return type;
    }

    public static MyTypeDescriptor valueOf( Class<?> type) {
        if (type == null) {
            type = Object.class;
        }
        MyTypeDescriptor desc = commonTypesCache.get(type);
        return (desc != null ? desc : new MyTypeDescriptor(MyResolvableType.forClass(type)));
    }
  }
  ```

- MyResolvableType的实现如下，主要是用来封装原始类型

  ```java
  public class MyResolvableType {

    private Class<?> resolved;

    private final Type type;

    public static MyResolvableType forClass( Class<?> clazz) {
        return new MyResolvableType(clazz);
    }

    private MyResolvableType( Class<?> clazz) {
        this.resolved = (clazz != null ? clazz : Object.class);
        this.type = this.resolved;
    }

    public Class<?> toClass() {
        return resolve(Object.class);
    }

    public Class<?> resolve(Class<?> fallback) {
        return (this.resolved != null ? this.resolved : fallback);
    }
  }
  ```

- 获取泛型的类的工具类GenericUtil。JAVA的泛型相比C++好像若了很多呀，目前这个类还不能正常工作，希望老师可以指导一二

  ```java
  public abstract class GenericUtil<T> {
    private Class<T> tClass;

    GenericUtil() {

        Type type = getClass().getGenericSuperclass();
        Type trueType = ((ParameterizedType) type).getActualTypeArguments()[0];
        //下面这句话会抛异常，不知道为啥
        this.tClass = (Class<T>) trueType;
    }

    Class<T> getType(){
        return tClass;
    }
  }
  ```

  - 使用时通过匿名子类来达到目的, 位于MySimpleTypeConverter

  ```java
    @Override
    public T convert(String value) throws IllegalArgumentException, NullPointerException {
        GenericUtil<T> genericUtil = new GenericUtil<T>(){};
        return convert(value, genericUtil.getType());
    }
  ```

## 作业内容4

> 通过 org.eclipse.microprofile.config.Config 读取当前应用名称

```java
public class JavaConfigDemo {
    public static void main(String[] args) {
        JavaConfig config = new JavaConfig();
        System.out.println(config.getValue("application.name", String.class));

        //int id = config.getValue("id", Integer.class);
    }
}
```