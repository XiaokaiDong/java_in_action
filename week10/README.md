# 小马哥JAVA实战营第十周作业


## 作业内容


> 完善 @org.geektimes.projects.user.mybatis.annotation.EnableMyBatis 实现，尽可能多地注入 org.mybatis.spring.SqlSessionFactoryBean 中依赖的组件

### 解答

扩充老师课程上的代码

主要集中在如下两个类中

- org.geektimes.projects.user.mybatis.annotation.EnableMyBatis
- org.geektimes.projects.user.mybatis.annotation.MyBatisBeanDefinitionRegistrar

特殊的地方在于利用Spring框架提供的类型换换服务ConversionService实现从String到Properties的转换

```java
//下面的代码利用BeanWrapperImpl -> TypeConverterSupport
//                            -> TypeConverterDelegate -> ConversionService实现从String到Properties的转换
beanDefinitionBuilder.addPropertyValue("configurationProperties", (String) attributes.get("configurationProperties"));

```

还有Class属性的赋值，比如

```java
//attributes返回Object， Class is Object，所以可以这么传递
beanDefinitionBuilder.addPropertyValue("defaultEnumTypeHandler", attributes.get("defaultEnumTypeHandler"));
beanDefinitionBuilder.addPropertyValue("typeAliases", attributes.get("typeAliases"));

```