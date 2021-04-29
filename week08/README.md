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

上面代码中生成的两个WebSecurityConfigurerAdapter配置类中都对HttpSecurity实例进行了配置。在5.2.2中HttpSecurity实例的生成是在生成org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration#webSecurityExpressionHandler的过程中作为依赖生成的。但似乎不止这一个。

### HttpSecurity对象的生成

> 分析所用Spring Security的版本为5.2.2

- 处理配置类org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration

  - 加载Bean webSecurityExpressionHandler
  ```java
    @Bean
	@DependsOn(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME)
	public SecurityExpressionHandler<FilterInvocation> webSecurityExpressionHandler() {
		return webSecurity.getExpressionHandler();
	}
  ```

  - 可以看出webSecurityExpressionHandler依赖于AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME，即名为springSecurityFilterChain的Bean，这个Bean也定义在WebSecurityConfiguration类中。

  ```java
    /**
	 * Creates the Spring Security Filter Chain
	 * @return the {@link Filter} that represents the security filter chain
	 * @throws Exception
	 */
	@Bean(name = AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME)
	public Filter springSecurityFilterChain() throws Exception {
		boolean hasConfigurers = webSecurityConfigurers != null
				&& !webSecurityConfigurers.isEmpty();
		if (!hasConfigurers) {
			WebSecurityConfigurerAdapter adapter = objectObjectPostProcessor
					.postProcess(new WebSecurityConfigurerAdapter() {
					});
			webSecurity.apply(adapter);
		}
		return webSecurity.build();
	}
  ```

  - 调试执行时hasConfigurers == true，所以直接执行webSecurity.build()，导致org.springframework.security.config.annotation.AbstractConfiguredSecurityBuilder#doBuild这个模板方法被执行，验证了小马哥在课上所讲的内容。

  ```java
    protected final O doBuild() throws Exception {
		synchronized (configurers) {
			buildState = BuildState.INITIALIZING;

			beforeInit();
			init();  // <------------------ 此方法导致HttpSecurity实例的构造

			buildState = BuildState.CONFIGURING;

			beforeConfigure();
			configure();

			buildState = BuildState.BUILDING;

			O result = performBuild();

			buildState = BuildState.BUILT;

			return result;
		}
	}
  ```

  在org.springframework.security.config.annotation.AbstractConfiguredSecurityBuilder#init中，依次调用org.springframework.security.config.annotation.AbstractConfiguredSecurityBuilder#configurers中的SecurityConfigurer对当前对象即org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration#webSecurity进行配置。

  进行这个配置的实际类是org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerSecurityConfiguration，它的init方法委托其父类即WebSecurityConfigurerAdapter进行。

  那 `WebSecurityConfiguration#webSecurity` 是何时初始化的呢？同时，AbstractConfiguredSecurityBuilder#configurers中的SecurityConfigurer是何时添加的呢？

- WebSecurityConfiguration#webSecurity的初始化

  在WebSecurityConfiguration中搜索webSecurity，可以看到在 `WebSecurityConfiguration#setFilterChainProxySecurityConfigurer` 中对webSecurity进行了赋值。

  ```java

  ```

- AbstractConfiguredSecurityBuilder#configurers中包含如下的SecurityConfigurer对象。他们都是被@Configuration标记的配置类。

  - AuthorizationServerSecurityConfiguration，派生自WebSecurityConfigurerAdapter
  - ResourceServerConfiguration（应用自定义的一个配置类），派生自WebSecurityConfigurerAdapter
  - AuthenticationServerConfiguration（应用自定义的一个配置类），派生自WebSecurityConfiguration

  AbstractConfiguredSecurityBuilder#configurers是被AbstractConfiguredSecurityBuilder#add插入的SecurityConfigurer对象，而这个add方法也被 `WebSecurityConfiguration#setFilterChainProxySecurityConfigurer` 调用。

### WebSecurityConfiguration#setFilterChainProxySecurityConfigurer

WebSecurityConfiguration#setFilterChainProxySecurityConfigurer被@Autowired注解，所以这个方法的调用是在创建配置类Bean WebSecurityConfiguration的过程中处理@Autowired依赖时调起的。

> 此时的依赖注入方式为方法（参数）注入。

两个参数：

- ObjectPostProcessor<Object> objectPostProcessor
- List<SecurityConfigurer<Filter, WebSecurity>> webSecurityConfigurers

    其中第二个参数被打上了一个@Value注解
    ```java
    @Value("#{@autowiredWebSecurityConfigurersIgnoreParents.getWebSecurityConfigurers()}")
    ```

    即webSecurityConfigurers是名为autowiredWebSecurityConfigurersIgnoreParents的Bean的getWebSecurityConfigurers方法生成的。这个Bean也是在autowiredWebSecurityConfigurersIgnoreParents这个配置类中配置的，具体就是使用依赖查找在BeanFactory中收集所有类型为WebSecurityConfigurer的Bean

    ```java
    public List<SecurityConfigurer<Filter, WebSecurity>> getWebSecurityConfigurers() {
            List<SecurityConfigurer<Filter, WebSecurity>> webSecurityConfigurers = new ArrayList<>();
            Map<String, WebSecurityConfigurer> beansOfType = beanFactory
                    .getBeansOfType(WebSecurityConfigurer.class);
            for (Entry<String, WebSecurityConfigurer> entry : beansOfType.entrySet()) {
                webSecurityConfigurers.add(entry.getValue());
            }
            return webSecurityConfigurers;
        }
    ```