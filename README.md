# java_in_action

# 小马哥JAVA实战营作业

## Week01 

### 作业内容1

```
通过自研 Web MVC 框架实现（可以自己实现）一个用户注册，forward 到一个成功的页面（JSP 用法）

    /register
    
通过 Controller -> Service -> Repository 实现（数据库实现）
```

### 作业内容2

```
JDNI 的方式获取数据库源（DataSource），在获取 Connection
```

## Week02

### 作业内容

```
通过课堂上的简易版依赖注入和依赖查找，实现用户注册功能

- 通过 UserService 实现用户注册
- 注册用户需要校验
  - Id：必须大于 0 的整数
  - 密码：6-32 位
  - 电话号码：采用中国大陆方式（11 位校验）
```

## Week03

### 作业内容1

>整合 https://jolokia.org/
>  实现一个自定义 JMX MBean，通过 Jolokia 做 Servlet 代理
  
### 作业内容2

>扩展 org.eclipse.microprofile.config.spi.ConfigSource 实现，包括 OS 环境变量，以及本地配置文件


### 作业内容3

> 实现 org.eclipse.microprofile.config.spi.Converter 实现，提供 String 类型到简单类型的转换

### 作业内容4

> 通过 org.eclipse.microprofile.config.Config 读取当前应用名称

## Week04

### 作业内容1


> 完善 my dependency-injection 模块
> 脱离 web.xml 配置实现 ComponentContext 自动初始

### 作业内容2

> 完善 my-configuration 模块
>  Config 对象如何能被 my-web-mvc 使用

## Week05

### 作业内容1


> 修复本程序 org.geektimes.reactive.streams.DefaultSubscriber#onNext

### 作业内容2

> 继续完善 my-rest-client POST 方法

## Week06

### 作业内容1


> 提供一套抽象 API 实现对象的序列化和反序列化

### 作业内容2


> 通过 Lettuce 实现一套 Redis CacheManager 以及 Cache

## Week07

### 作业内容


> 使用 Spring Boot 来实现一个整合Gitee/或者GithubOAuth2 认证

## Week08

### 作业内容


> 如何解决多个 WebSecurityConfigurerAdapter Bean 配置相 互冲突的问题?
>
> 提示:假设有两个 WebSecurityConfigurerAdapter Bean 定 义，并且标注了不同的 @Order，其中一个关闭 CSRF，一个 开启 CSRF，那么最终结果如何确定?
>
> 背景:Spring Boot 场景下，自动装配以及自定义 Starter 方 式非常流行，部分开发人员掌握了 Spring Security 配置方 法，并且自定义了自己的实现，解决了 Order 的问题，然而 会出现不确定配置因素。

## Week09

### 作业内容1


> - 如何清除某个 Spring Cache 所有的 Keys 关联的对象
>   - 如果 Redis 中心化方案，Redis + Sentinel
>   - 如果 Redis 去中心化方案，Redis Cluster

### 作业内容2

> 如何将 RedisCacheManager 与 @Cacheable 注解打通?

## Week10

### 作业内容


> 完善 @org.geektimes.projects.user.mybatis.annotation.EnableMyBatis 实现，尽可能多地注入 org.mybatis.spring.SqlSessionFactoryBean 中依赖的组件

## Week11

### 作业内容

> 通过 Java 实现两种（以及）更多的一致性 Hash 算法
> （可选）实现服务节点动态更新

## Week12

### 作业内容

> 将上次 MyBatis @Enable 模块驱动，封装成 Spring Boot Starter 方式参考：MyBatis Spring Project 里面会有 Spring Boot 实现

## Week13


### 作业内容

> 基于文件系统为 Spring Cloud 提供 PropertySourceLocator 实现
> 配置文件命名规则(META-INF/config/default.properties 或者 META-INF/config/default.yaml)


## Week14

### 作业内容

>利用Redis实现Spring Cloud Bus 中的 BusBridge，避免强依赖于Spring Cloud Stream.
>客户端主要使用lettuce。其实RedisTemplate也实现了相关的功能。


## Week15

### 作业内容

>通过 GraalVM 将一个简单 Spring Boot 工程构建为 Native Image，要求：
> - 代码要自己手写 @Controller @RequestMapping("/helloworld")
> - 相关插件可以参考 Spring Native Samples

## Week16

### 作业内容

>将 Spring Boot 应用打包 Java Native 应用，再将该应用通过 Dockerfile 构建 Docker 镜像，部署到 Docker 容器中，并且成功运行，Spring Boot 应用的实现复杂度不做要求

## Week17

### 作业内容

毕业总结