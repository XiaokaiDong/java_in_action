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

条件为

```java
beanName.equals("mybatisAutoConfiguration")
```

- 先使用ParameterNameDiscoverer来找到MybatisAutoConfiguration构造函数的参数名——找不到
- 然后使用org.springframework.beans.factory.support.ConstructorResolver#createArgumentArray来创建参数
  - 调用org.springframework.beans.factory.support.DefaultListableBeanFactory#findAutowireCandidates根据类型在BeanFactory中找到参数类型的Bean的名字
