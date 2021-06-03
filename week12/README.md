# 小马哥JAVA实战营第12周作业





## 作业内容



> 将上次 MyBatis @Enable 模块驱动，封装成 Spring Boot Starter 方式参考：MyBatis Spring Project 里面会有 Spring Boot 实现





## 解答



老师给出了参考。自动装配一直是自己想搞懂的事情，既然老师给了参考，就先研究清楚一下。



### 下载mybatis的spring-boot-starter工程



在模块mybatis-spring-boot-autoconfigure下的spring.factories文件中有如下的配置



```

# Auto Configure

org.springframework.boot.autoconfigure.EnableAutoConfiguration=\

org.mybatis.spring.boot.autoconfigure.MybatisLanguageDriverAutoConfiguration,\

org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration

```



重点研究MybatisAutoConfiguration。



### MybatisAutoConfiguration



调制执行测试方法org.mybatis.spring.boot.autoconfigure.MybatisAutoConfigurationTest#testSingleCandidateDataSource，并在MybatisAutoConfiguration的构造函数内打上断点，可以看出因为MybatisAutoConfiguration是一个配置类，所以上下文刷新的时候，会创建对应的Bean，导致其构造函数被调用。那么，构造函数的参数是怎么来的？



调用过程主要是在创建Bean的过程中，autowiring构造器的参数，即AbstractAutowireCapableBeanFactory#autowireConstructor，如下：



- org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBean

  - org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#autowireConstructor

    - org.springframework.beans.factory.support.ConstructorResolver#autowireConstructor



#### 在ConstructorResolver#autowireConstructor上打上断点



断点条件为



```java

beanName.equals("mybatisAutoConfiguration")

```



- 先使用ParameterNameDiscoverer来找到MybatisAutoConfiguration构造函数的参数名——找不到

- 然后使用org.springframework.beans.factory.support.ConstructorResolver#createArgumentArray来创建参数

  - 调用org.springframework.beans.factory.support.ConstructorResolver#resolveAutowiredArgument

    - 调用org.springframework.beans.factory.support.DefaultListableBeanFactory#resolveDependency根据名字和类型使用依赖查找找到Bean实例



#### 即构造函数中的参数是在BeanFactory中依赖查找来的。那这些被依赖的Bean是如何被定义、初始化的？



- 首先列出MybatisAutoConfiguration构造器的参数列表



```java

  public MybatisAutoConfiguration(MybatisProperties properties, ObjectProvider<Interceptor[]> interceptorsProvider,

      ObjectProvider<TypeHandler[]> typeHandlersProvider, ObjectProvider<LanguageDriver[]> languageDriversProvider,

      ResourceLoader resourceLoader, ObjectProvider<DatabaseIdProvider> databaseIdProvider,

      ObjectProvider<List<ConfigurationCustomizer>> configurationCustomizersProvider) {

```



- 1、MybatisProperties Bean



  > 先给结论：



  > MybatisProperties由配置类MybatisAutoConfiguration上的注解@EnableConfigurationProperties引入。在Spring上下文启动的上下文后置处理阶段处理配置类时，会将配置类上@EnableConfigurationProperties注解内标注的类对应的BeanDefinition——这里即MybatisProperties——通过@EnableXXX注解关联的ImportBeanDefinitionRegistrar注册到上下文中（这里的实现类是EnableConfigurationPropertiesRegistrar）。



  MybatisProperties Bean由MybatisAutoConfiguration这个配置类通过注解@EnableConfigurationProperties引入



  ```java

  @org.springframework.context.annotation.Configuration

  @ConditionalOnClass({ SqlSessionFactory.class, SqlSessionFactoryBean.class })

  @ConditionalOnSingleCandidate(DataSource.class)

  @EnableConfigurationProperties(MybatisProperties.class)

  @AutoConfigureAfter({ DataSourceAutoConfiguration.class, MybatisLanguageDriverAutoConfiguration.class })

  public class MybatisAutoConfiguration implements InitializingBean {

  ```



  根据@EnableConfigurationProperties的一般原理，它是由EnableConfigurationPropertiesRegistrar进行处理的



  ```java

  @Target(ElementType.TYPE)

  @Retention(RetentionPolicy.RUNTIME)

  @Documented

  @Import(EnableConfigurationPropertiesRegistrar.class)

  public @interface EnableConfigurationProperties {

  ```



  关键方法EnableConfigurationPropertiesRegistrar#registerBeanDefinitions定义如下



  ```java

    @Override

    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {

      registerInfrastructureBeans(registry);

      registerMethodValidationExcludeFilter(registry);

      ConfigurationPropertiesBeanRegistrar beanRegistrar = new ConfigurationPropertiesBeanRegistrar(registry);

      getTypes(metadata).forEach(beanRegistrar::register);

    }

  ```

  在ConfigurationPropertiesBeanRegistrar#register(java.lang.Class<?>)上打上断点，调试运行MybatisAutoConfigurationTest#testSingleCandidateDataSource，可以得知，EnableConfigurationPropertiesRegistrar#registerBeanDefinitions的处理入口是AbstractApplicationContext#invokeBeanFactoryPostProcessors，即上下文后置处理阶段：



  - AbstractApplicationContext#refresh

  - `AbstractApplicationContext#invokeBeanFactoryPostProcessors`

  - PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors

    

    主要是处理各种`BeanFactoryPostProcessor`，其中有一种特殊的子类实现BeanDefinitionRegistryPostProcessor，使用依赖查找在BeanFactory中根据类型找到所有的Bean实例，此处找到的是名为“org.springframework.context.annotation.internalConfigurationAnnotationProcessor”，实际类型为`ConfigurationClassPostProcessor`的Bean，这是一个内部依赖，执行其postProcessBeanDefinitionRegistry方法。



    顾名思义，ConfigurationClassPostProcessor是处理配置类的，它会定义很多Bean，正好符合BeanFactoryPostProcessor的名字所暗示的含义。

  - 调用ConfigurationClassPostProcessor#postProcessBeanDefinitionRegistry

  - ……



    最终会调用ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsForConfigurationClass就是处理各种配置类，正好MybatisAutoConfiguration就是一个配置类，然后找到这个配置类上关联的所有ImportBeanDefinitionRegistrar，这里正好是我们在找寻的目标EnableConfigurationPropertiesRegistrar，它会把注解@EnableConfigurationProperties中指定的类即MybatisProperties注册成为一个Bean.



  > 相关知识：internalConfigurationAnnotationProcessor这样的配置Bean是从哪来的？

  >

  > 像盖房子一样，在上下文比如AnnotationConfigApplicationContext的创建过程中，需要创建一些最初的工具用来加载各种各样的Bean，工具有两种：

  > - 扫描Bean定义的扫描器ClassPathBeanDefinitionScanner

  >   

  > ClassPathBeanDefinitionScanner的构造函数会利用AnnotationConfigUtils.registerAnnotationConfigProcessors来注册一些内部依赖

  ```java

  public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment environment) {

    Assert.notNull(registry, "BeanDefinitionRegistry must not be null");

    Assert.notNull(environment, "Environment must not be null");

    this.registry = registry;

    this.conditionEvaluator = new ConditionEvaluator(registry, environment, null);

    AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);

  }

  ```

  >这些最原始的依赖非常特殊，即不是依赖查找来的，也不是依赖注入来的，而是使用RootBeanDefinition直接创建的，比如名为org.springframework.context.annotation.internalConfigurationAnnotationProcessor的依赖Bean

  ```java

  if (!registry.containsBeanDefinition(CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME)) {

    RootBeanDefinition def = new RootBeanDefinition(ConfigurationClassPostProcessor.class);

    def.setSource(source);

    beanDefs.add(registerPostProcessor(registry, def, CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME));

  }

  ```

  > - 读取Bean定义的读取器AnnotatedBeanDefinitionReader



  >   还可以用AnnotationConfigApplicationContext#register手动注册配置类而无需借助@Configuration注解

  ```java

  this.context.register(SingleCandidateDataSourceConfiguration.class, MybatisAutoConfiguration.class,PropertyPlaceholderAutoConfiguration.class);

  ```

- 2、其余的ObjectProvider参数

  也是使用DefaultListableBeanFactory#resolveDependency来解析的，只是这时直接返回一个DependencyObjectProvider类型的实例(下面代码第2个分支)

  ```java
  @Override
	@Nullable
	public Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName,
			@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {

		descriptor.initParameterNameDiscovery(getParameterNameDiscoverer());
		if (Optional.class == descriptor.getDependencyType()) {
			return createOptionalDependency(descriptor, requestingBeanName);
		}
		else if (ObjectFactory.class == descriptor.getDependencyType() ||
				ObjectProvider.class == descriptor.getDependencyType()) {
			return new DependencyObjectProvider(descriptor, requestingBeanName);
		}
		else if (javaxInjectProviderClass == descriptor.getDependencyType()) {
			return new Jsr330Factory().createDependencyProvider(descriptor, requestingBeanName);
		}
		else {
			Object result = getAutowireCandidateResolver().getLazyResolutionProxyIfNecessary(
					descriptor, requestingBeanName);
			if (result == null) {
				result = doResolveDependency(descriptor, requestingBeanName, autowiredBeanNames, typeConverter);
			}
			return result;
		}
	}
  ```

  > DependencyObjectProvider是DefaultListableBeanFactory的内部类，且实现了ObjectProvider接口；是一个DependencyDescriptor的包装类：
  
  ```java
  /**
	 * Serializable ObjectFactory/ObjectProvider for lazy resolution of a dependency.
	 */
	private class DependencyObjectProvider implements BeanObjectProvider<Object> {

		private final DependencyDescriptor descriptor;

		private final boolean optional;
    ...
  }
  ```

- 3、ResourceLoader类型的参数

  走DefaultListableBeanFactory#resolveDependency的最后一个else分支的DefaultListableBeanFactory#doResolveDependency
  应用上下文AnnotationConfigApplicationContext本身就实现了ResourceLoader接口，直接返回。

- 4、MybatisAutoConfiguration构造函数的运行

  - 对于MybatisProperties参数来说，已经存在于当前上下文中了。
  - 对于ObjectProvider类型的参数来说，调用DefaultListableBeanFactory.DependencyObjectProvider#getIfAvailable即ObjectProvider#getIfAvailable接口的实现，最终调用DefaultListableBeanFactory#doResolveDependency（即调用外部类的方法）。即最终还是调用了DefaultListableBeanFactory#doResolveDependency，但相当于“延迟了”一下。

    下面，解析一下DefaultListableBeanFactory#doResolveDependency

    - 调用DefaultListableBeanFactory#resolveMultipleBeans，依赖查找集合类型和映射类型（也即Listable）
     - 调用DefaultListableBeanFactory#findAutowireCandidates
       - 使用依赖查找根据类型进行查找
       - 在resolvableDependencies中查找（比如ResourceLoader、ApplicationEventPublisher、BeanFactory、ApplicationContext等）
    - 如果上一步没有找到，继续调用DefaultListableBeanFactory#findAutowireCandidates查找
    - 如果还没有找到，且当前依赖不是必须的，则返回一个空，否则抛出异常。
    ```java

    ```


### 涉及到的Spring 应用上下文知识点



#### 上下文创建

  ```java

  this.context = new AnnotationConfigApplicationContext()

  ```

  Spring会创建很多内部依赖，比如internalConfigurationAnnotationProcessor。这些依赖可以看做底层组件，也即非业务相关的组件。

    

- 手动注册配置类——业务相关的配置类

  ```java

  this.context.register(MybatisAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);

  ```



#### 上下文刷新

  ```java

  context.refresh();

  ```

  - …… （忽略其它不相关步骤）

  - BeanFactory后置处理——AbstractApplicationContext#invokeBeanFactoryPostProcessors

    委派PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors进行处理



    > 现在我们手边有了内部依赖，还有了若干业务相关的配置类，这里就开始处理这些配置类。



    - 从上下文中查找类型为BeanDefinitionRegistryPostProcessor的Bean，正好在上下文创建的时候，创建了一个ConfigurationClassPostProcessor的Bean，它实现了BeanDefinitionRegistryPostProcessor接口。



      - 首先处理实现了PriorityOrdered接口的BeanDefinitionRegistryPostProcessor。



        ConfigurationClassPostProcessor也实现了PriorityOrdered接口。



      - 然后处理实现了Ordered接口的BeanDefinitionRegistryPostProcessor。

      - 最后处理剩余的BeanDefinitionRegistryPostProcessor。



        处理都是调用PostProcessorRegistrationDelegate#invokeBeanDefinitionRegistryPostProcessors进行的。

        它调用BeanDefinitionRegistryPostProcessor（比如这里的ConfigurationClassPostProcessor）的postProcessBeanDefinitionRegistry方法。ConfigurationClassPostProcessor类如其名，主要内容是处理配置类的定义：



        >依次当前被处理的BeanDefinitionRegistry中的所有BeanDefinitionNames，然后找到对应的BeanDefinition，判断对应的BeanDefinition是不是配置类，原理就是寻找这个类是不是被注解@Configuration标注，如果是的话，取出这个注解上的相关值：
        
        >

        >如果Configuration#proxyBeanMethods 为true，将名为”org.springframework.context.annotation.ConfigurationClassPostProcessor.configurationClass“，值为BeanMetadataAttribute对象的属性设置到BeanDefinition上。BeanMetadataAttribute对象封装了同样的名字和值对象Object，它的值可能为"full".

        >

        >创建ConfigurationClassParser，解析所有找到的配置类，将BeanDefinitionHolder转换为ConfigurationClass。对于MybatisAutoConfiguration来说，直接使用其BeanDefinition包含的AnnotationMetadata和Bean的名字构造一个ConfigurationClass。

        >

        >对于上面创建好的ConfigurationClass，使用ConfigurationClassBeanDefinitionReader#loadBeanDefinitions读取。在ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsForConfigurationClass中有如下的逻辑：加载由@Import导入或配置类内嵌的配置类、配置类中由@Bean注解定义的Bean和使用@ImportResource导入的XML中定义的Bean（这几类是由ConfigurationClassParser#doProcessConfigurationClass扫描进来的）、由ImportBeanDefinitionRegistrar导入的Bean（对应@EnableXXX注解）。所有这些Bean定义BeanDefinition都会被放入`DefaultListableBeanFactory#beanDefinitionMap`属性中。还会把这个注册表中所有的Bean的名字放入`DefaultListableBeanFactory#beanDefinitionNames`中。

        > loadBeanDefinitionsFromRegistrars的原理参考下面

        ```java

        if (configClass.isImported()) {

			    registerBeanDefinitionForImportedConfigurationClass(configClass);

		    }

		    for (BeanMethod beanMethod : configClass.getBeanMethods()) {

			    loadBeanDefinitionsForBeanMethod(beanMethod);

		    }

        

		    loadBeanDefinitionsFromImportedResources(configClass.getImportedResources());

		    loadBeanDefinitionsFromRegistrars(configClass.getImportBeanDefinitionRegistrars());

        ```

        >在上面的逻辑之前还有一个当前配置类是否应该加载的判断

        ```java

        if (trackedConditionEvaluator.shouldSkip(configClass)) {

        }

        ```

        >ConfigurationClassBeanDefinitionReader.TrackedConditionEvaluator#shouldSkip会递归判断当前配置类、当前配置类如果是内部类，则判断外部类，如果当前配置类是被@Imported引入的，判断@Imported所在的配置类。正好MybatisAutoConfiguration上有两个@Conditional注解

        ```java

        @org.springframework.context.annotation.Configuration

        @ConditionalOnClass({ SqlSessionFactory.class, SqlSessionFactoryBean.class })

        @ConditionalOnSingleCandidate(DataSource.class)

        @EnableConfigurationProperties(MybatisProperties.class)

        @AutoConfigureAfter({ DataSourceAutoConfiguration.class, MybatisLanguageDriverAutoConfiguration.class })

        public class MybatisAutoConfiguration implements InitializingBean {

        }

        ```

        >Spring会将@ConditionalOnClass和@ConditionalOnSingleCandidate注解定义中指明的类加载到当前JVM（它们都是被@Conditional”元注解“的注解，相当于是@Conditional的”子注解“）

        ```java

        @Target({ ElementType.TYPE, ElementType.METHOD })

        @Retention(RetentionPolicy.RUNTIME)

        @Documented

        @Conditional(OnClassCondition.class)

        public @interface ConditionalOnClass {

        }



        @Target({ ElementType.TYPE, ElementType.METHOD })

        @Retention(RetentionPolicy.RUNTIME)

        @Documented

        @Conditional(OnBeanCondition.class)

        public @interface ConditionalOnSingleCandidate {

        }

        ```

        >根据上面的代码，对于MybatisAutoConfiguration来说，会加载并初始化OnClassCondition实例和OnBeanCondition实例。

        >对于OnBeanCondition和对应的@ConditionalOnSingleCandidate注解来说，Spring利用依赖查找在当前上下文中查找相应的Bean。如果找到了，则当前配置类应该被加载，否则不进行加载。

        >特别的，对于properties文件中若干配置项引起的自动装配很感兴趣。按照和上面一样的套路，找到@ConditionalOnProperty的定义
        ```java
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ ElementType.TYPE, ElementType.METHOD })
        @Documented
        @Conditional(OnPropertyCondition.class)
        public @interface ConditionalOnProperty {
        }
        ```
        >关注OnPropertyCondition类。其原理就是先收集@ConditionalOnProperty注解的name或者value属性，然后利用上下文中的Environment（PropertyResolver的子接口）依赖查找所需的属性是否存在，如果存在则测试通过，否则不通过。

        - loadBeanDefinitionsFromRegistrars的原理——以@EnableConfigurationProperties为例

          调用相应@EnableXXX注解对应的类，比如@EnableConfigurationProperties对应的EnableConfigurationPropertiesRegistrar类的registerBeanDefinitions方法

          ```java
          @Override
          public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
            registerInfrastructureBeans(registry);
            registerMethodValidationExcludeFilter(registry);
            ConfigurationPropertiesBeanRegistrar beanRegistrar = new ConfigurationPropertiesBeanRegistrarConfigurationPropertiesBeanRegistrar(registry);
            getTypes(metadata).forEach(beanRegistrar::register);
          }
          ```

          其中EnableConfigurationPropertiesRegistrar#registerInfrastructureBeans会调用ConfigurationPropertiesBindingPostProcessor#register在上下文中注入工具Bean ConfigurationPropertiesBindingPostProcessor、ConfigurationPropertiesBinder.Factory、ConfigurationPropertiesBinder和BoundConfigurationProperties，即InfrastructureBeans
          ```java
          public static void register(BeanDefinitionRegistry registry) {
            Assert.notNull(registry, "Registry must not be null");
            if (!registry.containsBeanDefinition(BEAN_NAME)) {
              BeanDefinition definition = BeanDefinitionBuilder
                  .genericBeanDefinition(ConfigurationPropertiesBindingPostProcessor.class,
                      ConfigurationPropertiesBindingPostProcessor::new)
                  .getBeanDefinition();
              definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
              registry.registerBeanDefinition(BEAN_NAME, definition);
            }
            ConfigurationPropertiesBinder.register(registry);
          }
          ```

          registerMethodValidationExcludeFilter会注册InfrastructureBeans——MethodValidationExcludeFilter，顾名思义，是一个过滤器，过滤的条件是被注解@ConfigurationProperties标注。
          ```java
          static void registerMethodValidationExcludeFilter(BeanDefinitionRegistry registry) {
            if (!registry.containsBeanDefinition(METHOD_VALIDATION_EXCLUDE_FILTER_BEAN_NAME)) {
              BeanDefinition definition = BeanDefinitionBuilder
                  .genericBeanDefinition(MethodValidationExcludeFilter.class,
                      () -> MethodValidationExcludeFilter.byAnnotation(ConfigurationProperties.class))
                  .setRole(BeanDefinition.ROLE_INFRASTRUCTURE).getBeanDefinition();
              registry.registerBeanDefinition(METHOD_VALIDATION_EXCLUDE_FILTER_BEAN_NAME, definition);
            }
          }
          ```

          创建ConfigurationPropertiesBeanRegistrar，然后获取注解中的类名，比如`@EnableConfigurationProperties(MybatisProperties.class)`中的MybatisProperties，然后获取目标类比如MybatisProperties上的@ConfigurationProperties注解；然后注册类型MybatisProperties对应的BeanDefinition.


      - 调用PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors调起BeanDefinitionRegistryPostProcessor#postProcessBeanFactory（继承自BeanFactoryPostProcessor），即ConfigurationClassPostProcessor#postProcessBeanFactory

        在这里对标注了@Configuration的配置类进行CGLib加强，然后向BeanFactory中增加了ImportAwareBeanPostProcessor这种BeanPostProcessor。这里主要关心前者。

        调用ConfigurationClassPostProcessor#enhanceConfigurationClasses标注了@Configuration的配置类进行CGLib加强。

        - 主要依赖ConfigurationClassEnhancer#enhance
          - 调用ConfigurationClassEnhancer#newEnhancer创建Enhancer实例，并设置其回调为BeanMethodInterceptor和BeanFactoryAwareMethodInterceptor，它俩都实现了MethodInterceptor接口，应该分别是针对@Bean标记的方法调用和实现了BeanFactoryAware#setBeanFactory方法的setBeanFactory方法调用。
            
            BeanFactoryAwareMethodInterceptor主要是拦截BeanFactoryAware#setBeanFactory，会将传入的BeanFactory参数的值赋值给代理产生的CBLib子类的名为$$beanFactory的字段。

            BeanMethodInterceptor的主要作用是调用 @Bean 方法的时候，现在BeanFactory里查一遍，有则返回，没有则创建新的，就像javadoc所说：
            ```java
            /**
              * Enhance a {@link Bean @Bean} method to check the supplied BeanFactory for the
              * existence of this bean object.
              * @throws Throwable as a catch-all for any exception that may be thrown when invoking the
              * super implementation of the proxied method i.e., the actual {@code @Bean} method
              */
              @Override
              @Nullable
              public Object intercept(Object enhancedConfigInstance, Method beanMethod, Object[] beanMethodArgs,
                    MethodProxy cglibMethodProxy) throws Throwable {
              }
            ```

            > 当配置类上有@Configuration注解时，执行依赖查找时@Bean方法，会返回同一个Bean实例；如果没有，则每次查找都会返回一个新的实例。


      - 调用PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors处理BeanFactoryPostProcessor类型的Bean



      > BeanDefinitionRegistryPostProcessor是BeanFactoryPostProcessor的子接口。



    - 从上下文中查找类型为BeanFactoryPostProcessor的Bean。



      - 首先处理实现了PriorityOrdered接口的BeanFactoryPostProcessor。

      - 然后处理实现了Ordered接口的BeanFactoryPostProcessor。

      - 最后处理剩余的BeanFactoryPostProcessor。



      处理都是调用PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors进行的。



    - 配置类处理主要方法——ConfigurationClassParser#doProcessConfigurationClass


##### BeanDefinition的处理

分为两大块步骤：AbstractApplicationContext#invokeBeanFactoryPostProcessors和AbstractApplicationContext#registerBeanPostProcessors。

这两大块都委托给了PostProcessorRegistrationDelegate来处理。

- AbstractApplicationContext#invokeBeanFactoryPostProcessors

  >委托给PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors。
  
  上面讲到ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsForConfigurationClass会加载由@Import导入或配置类内嵌的配置类、配置类中由@Bean注解定义的Bean和使用@ImportResource导入的XML中定义的Bean（这几类是由ConfigurationClassParser#doProcessConfigurationClass 扫描进来的）、由ImportBeanDefinitionRegistrar导入的Bean（对应@EnableXXX注解）。所有这些Bean定义BeanDefinition都会被放入`DefaultListableBeanFactory#beanDefinitionMap`属性中。

  此时的BeanDefinition就是创建Bean实例的蓝本。对于配置类，比如MybatisAutoConfiguration，比如在依赖查找的时候，会调用

  - DefaultListableBeanFactory#doGetBeanNamesForType
    - AbstractBeanFactory#getMergedBeanDefinition(BeanDefinition)
      参数BeanDefinition就是`DefaultListableBeanFactory#beanDefinitionMap`中得到的，此时它的具体类型还是AnnotatedGenericBeanDefinition。
      然后在AbstractBeanFactory#getMergedBeanDefinition(String,BeanDefinition, BeanDefinition)中根据AnnotatedGenericBeanDefinition构造一个RootBeanDefinition，并将其放入AbstractBeanFactory#mergedBeanDefinitions中。

- AbstractApplicationContext#registerBeanPostProcessors

  >委托给PostProcessorRegistrationDelegate.registerBeanPostProcessors。

  在上下文中寻找所有的类型为BeanPostProcessor的Bean，在依赖查找的过程中（和上一小节的处理过程类似），会调用AbstractBeanFactory#getMergedLocalBeanDefinition。只是这时可以直接从mergedBeanDefinitions获取到RootBeanDefinition。
  ```java
  protected RootBeanDefinition getMergedLocalBeanDefinition(String beanName) throws BeansException {
		// Quick check on the concurrent map first, with minimal locking.
		RootBeanDefinition mbd = this.mergedBeanDefinitions.get(beanName);
		if (mbd != null && !mbd.stale) {
			return mbd;
		}
		return getMergedBeanDefinition(beanName, getBeanDefinition(beanName));
	}
  ```

- ConfigurationClassParser#doProcessConfigurationClass

  有如下的处理逻辑。

  - 处理@Component
  - 处理@PropertySource annotations
  - 处理@ComponentScan annotations
  - 处理@Import annotations

    会递归扫描所有被@Import的类，以及通过@Import被引入的类上额外的@Import二次引入的类，以此类推。
    会分成如下三种情况：

    - `candidate.isAssignable(ImportSelector.class)`
    - `candidate.isAssignable(ImportBeanDefinitionRegistrar.class)`
    - 普通的配置类

    - 如果有被@Import引入的ImportSelector，而这个ImportSelector又引入了EnableAutoConfiguration则会导致spring.factories被处理，调用路径如下
    
      - ConfigurationClassParser#parse(Set<BeanDefinitionHolder>)
        - ConfigurationClassParser.DeferredImportSelectorHandler#process
          - ConfigurationClassParser.DeferredImportSelectorGroupingHandler#processGroupImports
          - ConfigurationClassParser.DeferredImportSelectorGrouping#getImports
          - AutoConfigurationImportSelector.AutoConfigurationGroup#process
          - AutoConfigurationImportSelector#getAutoConfigurationEntry
          - AutoConfigurationImportSelector#getCandidateConfigurations
          - SpringFactoriesLoader#loadFactoryNames
      

      >注意：DeferredImportSelectorHandler这个类中有一个延迟的含义("Deferred")，它的主要作用是来处理所有的DeferredImportSelector，相当于是DeferredImportSelector的“容器”
      ```java
      private class DeferredImportSelectorHandler {

        @Nullable
        private List<DeferredImportSelectorHolder> deferredImportSelectors = new ArrayList<>();
      }
      ```
      >在其方法ConfigurationClassParser.DeferredImportSelectorHandler#handle中将DeferredImportSelector放到这个“容器”中，主要看下面的else分支
      ```java
      /**
      * Handle the specified {@link DeferredImportSelector}. If deferred import
      * selectors are being collected, this registers this instance to the list. If
      * they are being processed, the {@link DeferredImportSelector} is also processed
      * immediately according to its {@link DeferredImportSelector.Group}.
      * @param configClass the source configuration class
      * @param importSelector the selector to handle
      */
      public void handle(ConfigurationClass configClass, DeferredImportSelector importSelector) {
        DeferredImportSelectorHolder holder = new DeferredImportSelectorHolder(configClass, importSelector);
        if (this.deferredImportSelectors == null) {
          DeferredImportSelectorGroupingHandler handler = new DeferredImportSelectorGroupingHandler();
          handler.register(holder);
          handler.processGroupImports();
        }
        else {
          this.deferredImportSelectors.add(holder);
        }
      }
      ```
      >而handle方法是由处理@Imports的ConfigurationClassParser#processImports调用的，当被@Imports引入的是一个DeferredImportSelector实例时，就会被加入到“容器”中。
      >那么，“延迟”体现在什么地方呢？主要体现在ConfigurationClassParser#parse(Set<BeanDefinitionHolder>  configCandidates)处理完所有的应用定义的配置类后才处理DeferredImportSelector
      ```java
      public void parse(Set<BeanDefinitionHolder> configCandidates) {
        for (BeanDefinitionHolder holder : configCandidates) {
          //处理所有的配置类Bean定义 
          ...
        }

        //延迟处理DeferredImportSelector
        this.deferredImportSelectorHandler.process();
      }
      ```
      >DeferredImportSelector的文档中也特别做了说明
      ```java
      /**
      * A variation of {@link ImportSelector} that runs after all {@code @Configuration} beans
      * have been processed. This type of selector can be particularly useful when the selected
      * imports are {@code @Conditional}.
      *
      * <p>Implementations can also extend the {@link org.springframework.core.Ordered}
      * interface or use the {@link org.springframework.core.annotation.Order} annotation to
      * indicate a precedence against other {@link DeferredImportSelector DeferredImportSelectors}.
      *
      * <p>Implementations may also provide an {@link #getImportGroup() import group} which
      * can provide additional sorting and filtering logic across different selectors.
      *
      * @author Phillip Webb
      * @author Stephane Nicoll
      * @since 4.0
      */
      public interface DeferredImportSelector extends ImportSelector {
      ```
      >Spring自动配置应该也是处理完业务的配置类后再根据用户的配置引入一些自动配置类，符合“延迟”的含义——AutoConfigurationImportSelector。
      
      - 自动配置类名字被加载后，会被spring.factories中定义的过滤器过滤，如下

        ># Auto Configuration Import Filters
        >org.springframework.boot.autoconfigure.AutoConfigurationImportFilter=\
        >org.springframework.boot.autoconfigure.condition.OnBeanCondition,\
        >org.springframework.boot.autoconfigure.condition.OnClassCondition,\
        >org.springframework.boot.autoconfigure.condition.OnWebApplicationCondition

      - 然后又使用SpringFactoriesLoader.loadFactories机制，在spring.factories中加载AutoConfigurationImportListener的实现类

        ># Auto Configuration Import Listeners
        >org.springframework.boot.autoconfigure.AutoConfigurationImportListener=\
        >org.springframework.boot.autoconfigure.condition.ConditionEvaluationReportAutoConfigurationImportListener

        向其发送AutoConfigurationImportEvent事件, AutoConfigurationImportSelector#fireAutoConfigurationImportEvents
        ```java
        private void fireAutoConfigurationImportEvents(List<String> configurations, Set<String> exclusions) {
          List<AutoConfigurationImportListener> listeners = getAutoConfigurationImportListeners();
          if (!listeners.isEmpty()) {
            AutoConfigurationImportEvent event = new AutoConfigurationImportEvent(this, configurations, exclusions);
            for (AutoConfigurationImportListener listener : listeners) {
              invokeAwareMethods(listener);
              listener.onAutoConfigurationImportEvent(event);
            }
          }
        }
        ```

      - 加载所有的自动配置类名字后，会在AutoConfigurationImportSelector.AutoConfigurationGroup#selectImports中对它们进行排序

        AutoConfigurationImportSelector.AutoConfigurationGroup#selectImports
        AutoConfigurationImportSelector.AutoConfigurationGroup#sortAutoConfigurations

        在这个过程中，会处理大量类似@AutoConfigureAfter、@AutoConfigureBefore之类的注解。

  - 处理@ImportResource annotations
  - 处理@Bean annotations

  >相应的，上面找到的内容，接下来会被ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsForConfigurationClass处理
  ```java
  /**
	 * Read a particular {@link ConfigurationClass}, registering bean definitions
	 * for the class itself and all of its {@link Bean} methods.
	 */
	private void loadBeanDefinitionsForConfigurationClass(
			ConfigurationClass configClass, TrackedConditionEvaluator trackedConditionEvaluator) {

		if (trackedConditionEvaluator.shouldSkip(configClass)) {
			String beanName = configClass.getBeanName();
			if (StringUtils.hasLength(beanName) && this.registry.containsBeanDefinition(beanName)) {
				this.registry.removeBeanDefinition(beanName);
			}
			this.importRegistry.removeImportingClass(configClass.getMetadata().getClassName());
			return;
		}

		if (configClass.isImported()) {
			registerBeanDefinitionForImportedConfigurationClass(configClass);
		}
		for (BeanMethod beanMethod : configClass.getBeanMethods()) {
			loadBeanDefinitionsForBeanMethod(beanMethod);
		}

		loadBeanDefinitionsFromImportedResources(configClass.getImportedResources());
		loadBeanDefinitionsFromRegistrars(configClass.getImportBeanDefinitionRegistrars());
	}
  ```



##### BeanDefinition 的注册

- AnnotatedBeanDefinitionReader#registerBean
  - AnnotatedBeanDefinitionReader#doRegisterBean

- AnnotatedBeanDefinitionReader#doRegisterBean

  首先利用被注册的Bean的类型初始化一个AnnotatedGenericBeanDefinition，然后直接评估是否应该跳过
  ```java
  AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(beanClass);
  if (this.conditionEvaluator.shouldSkip(abd.getMetadata())) {
    return;
  }
  ```

  上面的判断没有命中，所以BeanDefinition被放入`DefaultListableBeanFactory#beanDefinitionNames`中。

- 对于配置类来说，它也是一个Bean，还需要经过ConfigurationClassPostProcessor#postProcessBeanDefinitionRegistry的处理

  ConfigurationClassPostProcessor#postProcessBeanDefinitionRegistry会调用
  
  - ConfigurationClassBeanDefinitionReader#loadBeanDefinitions
    - ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsForConfigurationClass

  在ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsForConfigurationClass中会对是否应该加载配置类中定义的Bean进行判断

  ```java
  if (trackedConditionEvaluator.shouldSkip(configClass)) {
    String beanName = configClass.getBeanName();
    if (StringUtils.hasLength(beanName) && this.registry.containsBeanDefinition(beanName)) {
      this.registry.removeBeanDefinition(beanName);
    }
    this.importRegistry.removeImportingClass(configClass.getMetadata().getClassName());
    return;
  }
  ```

  ConfigurationClassBeanDefinitionReader.TrackedConditionEvaluator#shouldSkip会调用ConditionEvaluator#shouldSkip(AnnotatedTypeMetadata, ConfigurationCondition.ConfigurationPhase)，后者会判断@Conditional系列注解是否得到满足，从而决定是否应该加载配置类中定义的Bean。如果决定不处理，则会将当前配置类Bean的名字从`DefaultListableBeanFactory#beanDefinitionNames`中去除。