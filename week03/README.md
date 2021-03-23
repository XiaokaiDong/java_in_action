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

> 实现 org.eclipse.microprofile.config.spi.Converter 实现，提供 String 类型到简单类型的转换

- 使用MyAbstractTypeConverter实现org.eclipse.microprofile.config.spi.Converter接口

  ```java
    /**
     * 类型转换类，将String转换为基本类型的包装类
    * @param <T> 目标类型
    */
    public abstract class MyAbstractTypeConverter<T> implements Converter<T> {

        private final static Map<Class<?>, ThrowableFunction<String, Object>> converterMap = new HashMap<>();

        static {
            converterMap.putIfAbsent(Boolean.class, Boolean::parseBoolean);
            converterMap.putIfAbsent(Byte.class, Byte::parseByte);
            converterMap.putIfAbsent(Double.class, Double::valueOf);
            converterMap.putIfAbsent(Float.class, Float::valueOf);
            converterMap.putIfAbsent(Integer.class, Integer::valueOf);
            converterMap.putIfAbsent(Long.class, Long::valueOf);
            converterMap.putIfAbsent(Short.class, Short::valueOf);
        }

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
        public MyAbstractTypeConverter() {

        }


        @Override
        public T convert(String value) throws IllegalArgumentException, NullPointerException {
            if (value == null) {
                throw new NullPointerException("The value must not be null!");
            }
            return doConvert(value);
        }

        protected abstract T doConvert(String value);

        private void assertConverter(Converter<?> converter) {
            Class<?> converterClass = converter.getClass();
            if (converterClass.isInterface()) {
                throw new IllegalArgumentException("The implementation class of Converter must not be an interface!");
            }
            if (Modifier.isAbstract(converterClass.getModifiers())) {
                throw new IllegalArgumentException("The implementation class of Converter must not be abstract!");
            }
        }

        private Class<?> resolveConvertedType(Type type) {
            Class<?> convertedType = null;
            if (type instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) type;
                if (pType.getRawType() instanceof Class) {
                    Class<?> rawType = (Class) pType.getRawType();
                    if (Converter.class.isAssignableFrom(rawType)) {
                        Type[] arguments = pType.getActualTypeArguments();
                        if (arguments.length == 1 && arguments[0] instanceof Class) {
                            convertedType = (Class) arguments[0];
                        }
                    }
                }
            }
            return convertedType;
        }

        private Class<?> resolveConvertedType(Class<?> converterClass) {
            Class<?> convertedType = null;

            for (Type superInterface : converterClass.getGenericInterfaces()) {
                convertedType = resolveConvertedType(superInterface);
                if (convertedType != null) {
                    break;
                }
            }

            return convertedType;
        }

        public Class<?> resolveConvertedType(Converter<?> converter) {
            assertConverter(converter);
            Class<?> convertedType = null;
            Class<?> converterClass = converter.getClass();
            while (converterClass != null) {
                convertedType = resolveConvertedType(converterClass);
                if (convertedType != null) {
                    break;
                }

                Type superType = converterClass.getGenericSuperclass();
                if (superType instanceof ParameterizedType) {
                    convertedType = resolveConvertedType(superType);
                }

                if (convertedType != null) {
                    break;
                }
                // recursively
                converterClass = converterClass.getSuperclass();

            }

            return convertedType;
        }

        protected ThrowableFunction<String, Object> getConvertingFunc(Class<?> targetClass) {
            if (converterMap.containsKey(targetClass))
                return converterMap.get(targetClass);
            else
                return null;
        }
    }
  ```

  - 基于MyAbstractTypeConverter扩展几个具体的转换类，以ShortConverter举例

    ```java
    public class ShortConverter extends MyAbstractTypeConverter<Short> {

        @Override
        protected Short doConvert(String value) {
            ThrowableFunction<String, Object> converter = getConvertingFunc(resolveConvertedType(this));
            try {
                return (Short) converter.apply(value);
            } catch (Throwable throwable) {
                throw new IllegalArgumentException(throwable.getMessage());
            }
        }
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