# 小马哥JAVA实战营第14周作业


## 作业内容

>利用Redis实现Spring Cloud Bus 中的 BusBridge，避免强依赖于Spring Cloud Stream.
>客户端主要使用lettuce。其实RedisTemplate也实现了相关的功能。

## 解答

主要是利用Redis从5.0版本开始提供的 Streams 数据类型.实现在模块week14中。

>Streams 是 Redis 专门为消息队列设计的数据类型，它提供了丰富的消息队列操作命令。
>- XADD：插入消息，保证有序，可以自动生成全局唯一 ID；
>- XREAD：用于读取消息，可以按 ID 读取数据；
>- XREADGROUP：按消费者组形式读取消息；
>- XPENDING 和 XACK：XPENDING 命令可以用来查询每个消费组内所有消费者已读取但尚未确认的消息，而 XACK 命令用于向消息队列确认消息处理已完成。

### 基于Redis lettuce客户端的BusBridge的实现类BusBridge

#### 发送一条消息使用XADD命令

```java
    @Override
    public void send(RemoteApplicationEvent event) {


        Map<String, String> messageBody = new HashMap<>();
        try {
            messageBody.put(REMOTE_APPLICATION_EVENT_KEY, objectMapper.writeValueAsString(event));
            String messageId = syncCommands.xadd(REMOTE_EVENT_TOPIC, messageBody);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }
```

xadd命令用的消息队列名如下（命名上表示一个关于远程事件的主题）
```java
public static final String REMOTE_EVENT_TOPIC = "topic:remote_event";
```

放入消息时用的键如下
```java
private final String REMOTE_APPLICATION_EVENT_KEY = "application_event";
```

#### 读取一条消息使用XREADGROUP命令

按消费者组形式读取消息。

```java
    public void readRemoteApplicationEvent(){

        try {
            // WARNING: Streams must exist before creating the group
            //          This will not be necessary in Lettuce 5.2, see https://github.com/lettuce-io/lettuce-core/issues/898
            syncCommands.xgroupCreate( XReadArgs.StreamOffset.from(REMOTE_EVENT_TOPIC, "0-0"), "application_1"  );
        }
            catch (RedisBusyException redisBusyException) {
            System.out.println( String.format("\t Group '%s already' exists","application_1"));
        }


        System.out.println("Waiting for new messages");

        List<StreamMessage<String, String>> messages = syncCommands.xreadgroup(
                Consumer.from("application_1", "consumer_1"),
                XReadArgs.StreamOffset.lastConsumed(REMOTE_EVENT_TOPIC)
        );

        if (!messages.isEmpty()) {
            for (StreamMessage<String, String> message : messages) {
                System.out.println(message);
                Map<String, String> body = message.getBody();
                for(Map.Entry<String, String> entry : body.entrySet()){
                    String key = entry.getKey();
                    String value = entry.getValue();
                    System.out.printf("the key is %s\n", key);
                    System.out.printf("the value is %s\n", value);
                    try {
                        EnvironmentChangeRemoteApplicationEvent remoteApplicationEvent = objectMapper.readValue(value, EnvironmentChangeRemoteApplicationEvent.class);
                        System.out.printf("the RemoteApplicationEvent received is %s\n", remoteApplicationEvent);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }

                // Confirm that the message has been processed using XACK
                syncCommands.xack(REMOTE_EVENT_TOPIC, "application_1",  message.getId());
            }
        }


    }
```

### 测试一下

```java
@Configuration
public class RedisBusBridgeDemo {
    public static void main(String[] args) {

        // 创建 BeanFactory 容器
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        // 注册 Configuration Class（配置类）
        applicationContext.register(RedisBusBridgeDemo.class);

        // 启动 Spring 应用上下文
        applicationContext.refresh();

        RedisBusBridgeDemo redisBusBridgeDemo = (RedisBusBridgeDemo)applicationContext.getBean("redisBusBridgeDemo");

        RedisClient redisClient = RedisClient.create("redis://password01!@22.16.221.152:6379"); // change to reflect your environment
        StatefulRedisConnection<String, String> connection = redisClient.connect();

        Map<String, String> nullMap = new HashMap<>();
        nullMap.put("key1", "value1");
        nullMap.put("key2", "value2");

        EnvironmentChangeRemoteApplicationEvent environmentChangeRemoteApplicationEvent =
                new EnvironmentChangeRemoteApplicationEvent(redisBusBridgeDemo,
                        "nullService", new Destination() {
                    @Override
                    public String getDestinationAsString() {
                        return RedisBusBridge.REMOTE_EVENT_TOPIC;
                    }
                },nullMap);

        RedisBusBridge redisBusBridge = new RedisBusBridge(connection);
        redisBusBridge.send(environmentChangeRemoteApplicationEvent);

        redisBusBridge.readRemoteApplicationEvent();

        connection.close();
        redisClient.shutdown();

        // 显示地关闭 Spring 应用上下文
        applicationContext.close();

    }


}
```

> 作业做完，再记一段笔记

## Spring Cloud中openfeign的原理



### 如何在上下文中产生被@FeignClient标记的接口类的BeanDefinition



在某个接口上打上@FeignClient注解



```java

@FeignClient("${echo.service.provider.application.name:spring-cloud-service-provider}")

public interface EchoService {



    @GetMapping("/echo/{message}")

    String echo(@PathVariable String message);

}

```



然后在程序中使用@EnableFeignClients激活Feign，并依赖注入一个被@FeignClient标注的EchoService类型的接口



```java

@SpringBootApplication

@EnableDiscoveryClient

@RestController

@EnableFeignClients(basePackages = "org.geektimes.projects.spring.cloud.service")

public class ServiceConsumer {



    @Autowired

    private EchoService echoService;



    ...

}

```



@EnableFeignClients关联的实现类为FeignClientsRegistrar



```java

@Retention(RetentionPolicy.RUNTIME)

@Target(ElementType.TYPE)

@Documented

@Import(FeignClientsRegistrar.class)

public @interface EnableFeignClients {

  ...

}

```



则在上下文启动处理配置类的过程中最终会调用到FeignClientsRegistrar#registerBeanDefinitions这个方法中，如下



- AbstractApplicationContext#invokeBeanFactoryPostProcessors

- ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsForConfigurationClass

- ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsFromRegistrars

- FeignClientsRegistrar#registerBeanDefinitions

  - FeignClientsRegistrar#registerFeignClients



最后，在FeignClientsRegistrar#registerFeignClients中：



- 取出@EnableFeignClients注解的所有属性，比如`basePackages = "org.geektimes.projects.spring.cloud.service"`

- 使用ClassPathScanningCandidateComponentProvider#findCandidateComponents在这个包下面扫描所有加了@FeignClient的接口（只能是接口，否则会报错），生成对应的BeanDefinition

- 调用FeignClientsRegistrar#registerFeignClients的重载之一，使用BeanDefinitionBuilder来构建BeanDefinition



  ```java

  private void registerFeignClient(BeanDefinitionRegistry registry, AnnotationMetadata annotationMetadata,

			Map<String, Object> attributes) {

		String className = annotationMetadata.getClassName();

		BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(FeignClientFactoryBean.class);

		validate(attributes);

		definition.addPropertyValue("url", getUrl(attributes));

		definition.addPropertyValue("path", getPath(attributes));

		String name = getName(attributes);

		definition.addPropertyValue("name", name);

		String contextId = getContextId(attributes);

		definition.addPropertyValue("contextId", contextId);

		definition.addPropertyValue("type", className);

		definition.addPropertyValue("decode404", attributes.get("decode404"));

		definition.addPropertyValue("fallback", attributes.get("fallback"));

		definition.addPropertyValue("fallbackFactory", attributes.get("fallbackFactory"));

		definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);



		String alias = contextId + "FeignClient";

		AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();

		beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, className);

    ...

  }

  ```

  >请注意，这个BeanDefinition对应的类型为FeignClientFactoryBean!!!!



  最关键的一步，在BeanDefinitionReaderUtils#registerBeanDefinition中将被@FeignClient标注的接口类，比如EchoService这个Bean的名字与上面对应FeignClientFactoryBean类型的BeanDefinition绑定在一起



  ```java

  registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());

  ```



### 实际实例化一个被@FeignClient标记的接口类的Bean实例



在DefaultSingletonBeanRegistry#getSingleton(String, boolean)中，以名字EchoService会得到一个FeignClientFactoryBean类型的Bean，然后调用AbstractBeanFactory#getObjectForBeanInstance，这个方法的JavaDoc如下：



>Get the object for the given bean instance, either the bean instance itself or its created object in case of a FactoryBean.



最终调用FeignClientFactoryBean#getTarget -> FeignClientFactoryBean#loadBalance -> Targeter#target去产生一个真正的Bean。

####   真正的feign.Client是怎么产生的

FeignClientFactoryBean#getTarget
  ->FeignClientFactoryBean#loadBalance
    ->NamedContextFactory#getInstance
	  ->DefaultListableBeanFactory#resolveBean

在DefaultListableBeanFactory#resolveBean中第一次找不到，只能在父上下文（可能是SpringCloud上下文）中寻找，会找到一个名为feignRetryClient的Bean，搜索feignRetryClient会找到3个Bean定义

```java
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LoadBalancerProperties.class)
class DefaultFeignLoadBalancerConfiguration {
}

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ApacheHttpClient.class)
@ConditionalOnBean({ LoadBalancerClient.class, LoadBalancerClientFactory.class })
@ConditionalOnProperty(value = "feign.httpclient.enabled", matchIfMissing = true)
@Import(HttpClientFeignConfiguration.class)
@EnableConfigurationProperties(LoadBalancerProperties.class)
class HttpClientFeignLoadBalancerConfiguration {
}

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(OkHttpClient.class)
@ConditionalOnProperty("feign.okhttp.enabled")
@ConditionalOnBean({ LoadBalancerClient.class, LoadBalancerClientFactory.class })
@Import(OkHttpFeignConfiguration.class)
@EnableConfigurationProperties(LoadBalancerProperties.class)
class OkHttpFeignLoadBalancerConfiguration {
}
```

分别对应默认客户端、ApacheHttpClient和OkHttpClient。



### 底层依赖的Http客户端的注册——注册FeignClientSpecification



应用代码打上@EnableFeignClients注解，就会在处理配置类时激活FeignClientsRegistrar类的运行



ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsForConfigurationClass

 -> ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsFromRegistrars

  -> ImportBeanDefinitionRegistrar#registerBeanDefinitions(实际类型为FeignClientsRegistrar#registerBeanDefinitions)

FeignClientsRegistrar#registerBeanDefinitions的代码如下：

```java

@Override

public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {

	registerDefaultConfiguration(metadata, registry);

	registerFeignClients(metadata, registry);

}

```

- registerDefaultConfiguration

  会调用registerClientConfiguration
  ```java
	private void registerClientConfiguration(BeanDefinitionRegistry registry, Object name, Object configuration) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(FeignClientSpecification.class);
		builder.addConstructorArgValue(name);
		builder.addConstructorArgValue(configuration);
		registry.registerBeanDefinition(name + "." + FeignClientSpecification.class.getSimpleName(),
				builder.getBeanDefinition());
	}
  ```
  上面的name就是"default" + 被@EnableFeignClients标注的类名，比如"default.org.geektimes.projects.spring.cloud.service.consumer.ServiceConsumer"，default表示这是一个默认的http客户端；Bean的类型是FeignClientSpecification。

- registerFeignClients

  - 取出注解@EnableFeignClients的"clients"属性；
  - 使用ClassPathScanningCandidateComponentProvider扫描@EnableFeignClients属性`basePackages = "org.geektimes.projects.spring.cloud.service"`下的所有标记@FeignClient注解的类；
  - 对于扫描到的类，生成ScannedGenericBeanDefinition，存入LinkedHashSet结构中；
  - 获取@FeignClient注解上的属性
  - 调用registerClientConfiguration（代码在registerDefaultConfiguration小节中给出），Bean定义的名字取自如下代码
    >@FeignClient("${echo.service.provider.application.name:spring-cloud-service-provider}")
  - Bean的类型也是FeignClientSpecification。Bean定义所用的configuration取自@FeignClient注解的FeignClient#configuration属性
  - 调用FeignClientsRegistrar#registerFeignClient
    - 创建类型为FeignClientFactoryBean的BeanDefinition
	- 将@FeignClient注解的属性放入BeanDefinition中，比如`definition.addPropertyValue("url", getUrl(attributes));`
	- 将这个BeanDefinition标记上FactoryBean属性(FactoryBean#OBJECT_TYPE_ATTRIBUTE = "factoryBeanObjectType")

	  ```java
	  beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, className);
	  ```
