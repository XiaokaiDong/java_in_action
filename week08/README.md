# 小马哥JAVA实战营第八周作业


## 作业内容


> 如何解决多个 WebSecurityConfigurerAdapter Bean 配置相 互冲突的问题?
>
> 提示:假设有两个 WebSecurityConfigurerAdapter Bean 定 义，并且标注了不同的 @Order，其中一个关闭 CSRF，一个 开启 CSRF，那么最终结果如何确定?
>
> 背景:Spring Boot 场景下，自动装配以及自定义 Starter 方 式非常流行，部分开发人员掌握了 Spring Security 配置方 法，并且自定义了自己的实现，解决了 Order 的问题，然而 会出现不确定配置因素。

- 没有思路，好难啊，这里只把自己思考的东西写下来

- 没有用过Spring Security，这两天突击搜索了一些资料，主要是比较详细的看了如下的系列文章：

> https://mp.weixin.qq.com/mp/appmsgalbum?__biz=MzI1NDY0MTkzNQ==&action=getalbum&album_id=1319828555819286528&scene=173&from_msgid=2247489302&from_itemidx=2&count=3&nolastread=1#wechat_redirect

### WebSecurity 和 HttpSecurity 的区别

- HttpSecurity 目的是构建过滤器链，一个 HttpSecurity 对象构建一条过滤器链，一个过滤器链中有 N 个过滤器，HttpSecurity 所做的事情实际上就是在配置这 N 个过滤器。

- WebSecurity 目的是构建 FilterChainProxy，一个 FilterChainProxy 中包含有多个过滤器链和一个 Firewall。

- HttpSecurity是DefaultSecurityFilterChain的builder. 

### 对问题的理解

下面的代码中配置了两个WebSecurityConfigurerAdapter配置类（配置类也是Bean）DefaultWebSecurityConfig和DefaultWebSecurityConfig2.

```java
@Configuration
public class SecurityConfig {
    @Bean
    protected UserDetailsService userDetailsService() {
        //...
    }

    @Configuration
    @Order(1)
    static class DefaultWebSecurityConfig extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.authorizeRequests()
                    .authorizeRequests()
                    .anyRequest().hasRole("admin")
                    .and()
                    .csrf().disable();
        }
    }

    @Configuration
    @Order(2)
    static class DefaultWebSecurityConfig2 extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.authorizeRequests()
                    .authorizeRequests()
                    .anyRequest().hasRole("user")
                    .and()
                    .formLogin()
                    .permitAll()
                    .and()
                    .csrf().disable();
        }
    }
}
```

上面代码中生成的两个WebSecurityConfigurerAdapter配置类中都对HttpSecurity实例进行了配置。在5.2.2中HttpSecurity实例的生成是在生成org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration#webSecurityExpressionHandler的过程中作为依赖生成的。但似乎不止这一个，