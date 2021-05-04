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

