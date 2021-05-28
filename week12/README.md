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
>   ClassPathBeanDefinitionScanner的构造函数会利用AnnotationConfigUtils.registerAnnotationConfigProcessors来注册一些内部依赖
> ```java
> 	public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment environment) {
>		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
>		Assert.notNull(environment, "Environment must not be null");
>		this.registry = registry;
>		this.conditionEvaluator = new ConditionEvaluator(registry, environment, null);
>		AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
>	}
> ```
>   这些最原始的依赖非常特殊，即不是依赖查找来的，也不是依赖注入来的，而是使用RootBeanDefinition直接创建的，比如名为org.springframework.context.annotation.internalConfigurationAnnotationProcessor的依赖Bean
>```java
>if (!registry.containsBeanDefinition(CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME)) {
>	RootBeanDefinition def = new RootBeanDefinition(ConfigurationClassPostProcessor.class);
>	def.setSource(source);
>	beanDefs.add(registerPostProcessor(registry, def, CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME));
>}
>```
> - 读取Bean定义的读取器AnnotatedBeanDefinitionReader

> 还可以用AnnotationConfigApplicationContext#register手动注册配置类而无需借助@Configuration注解
> ```java
>
> ```

### 涉及到的Spring 应用上下文知识点

- 上下文创建
  - 注册了很多配置类，比如internalConfigurationAnnotationProcessor