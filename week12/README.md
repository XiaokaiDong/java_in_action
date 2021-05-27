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

调制执行测试方法org.mybatis.spring.boot.autoconfigure.MybatisAutoConfigurationTest#testSingleCandidateDataSource，并在MybatisAutoConfiguration的构造函数内打上断点，可以看出调用的过程是应用上下文刷新的时候，依赖查找名为mybatisAutoConfiguration的Bean，导致构造函数被调用，那么，构造函数的参数是怎么来的？

调用链如下：

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

#### 即构造函数中的参数是在BeanFactory中依赖查找来的。那这些被依赖的Bean是如何被初始化的？

- MybatisAutoConfiguration构造器的参数列表

```java
  public MybatisAutoConfiguration(MybatisProperties properties, ObjectProvider<Interceptor[]> interceptorsProvider,
      ObjectProvider<TypeHandler[]> typeHandlersProvider, ObjectProvider<LanguageDriver[]> languageDriversProvider,
      ResourceLoader resourceLoader, ObjectProvider<DatabaseIdProvider> databaseIdProvider,
      ObjectProvider<List<ConfigurationCustomizer>> configurationCustomizersProvider) {
```

- MybatisProperties Bean

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
在ConfigurationPropertiesBeanRegistrar#register(java.lang.Class<?>)上打上断点，调试运行MybatisAutoConfigurationTest#testSingleCandidateDataSource，可以得知，EnableConfigurationPropertiesRegistrar#registerBeanDefinitions的处理入口如下：

- AbstractApplicationContext#refresh
- AbstractApplicationContext#invokeBeanFactoryPostProcessors
- PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors
  
  主要是处理各种BeanFactoryPostProcessor，其中有一种特殊的子类实现BeanDefinitionRegistryPostProcessor，使用依赖查找在BeanFactory中根据类型找到所有的Bean实例，此处找到的是名为“org.springframework.context.annotation.internalConfigurationAnnotationProcessor”，实际类型为ConfigurationClassPostProcessor的Bean，这是一个内部依赖，执行其postProcessBeanDefinitionRegistry方法。

  顾名思义，ConfigurationClassPostProcessor是处理配置类的，它会定义很多Bean，正好符合BeanFactoryPostProcessor的名字所暗示的含义。
- 调用ConfigurationClassPostProcessor#postProcessBeanDefinitionRegistry
- ……

最终会调用ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsForConfigurationClass就是处理各种配置类，正好MybatisAutoConfiguration就是一个配置类，然后找到这个配置类上关联的所有ImportBeanDefinitionRegistrar，这里正好是我们在找寻的目标EnableConfigurationPropertiesRegistrar，它会把注解@EnableConfigurationProperties中指定的类即MybatisProperties注册成为一个Bean.