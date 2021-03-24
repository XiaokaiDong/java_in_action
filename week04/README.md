# 小马哥JAVA实战营第四周作业


## 作业内容1


> 完善 my dependency-injection 模块
>  脱离 web.xml 配置实现 ComponentContext 自动初始


- 新建模块my-dependency-injection

  - 将之前config的内容重构到此模块下，新增ApplicationContextInitializer类实现ServletContainerInitializer来使用编程的方式添加ComponentContextInitializerListener

  ```java
  public class ApplicationContextInitializer implements ServletContainerInitializer {
    @Override
    public void onStartup(Set<Class<?>> c, ServletContext servletContext) throws ServletException {
        servletContext.addListener(ComponentContextInitializerListener.class);
    }
  }
  ```

  - 使用  ComponentContextInitializerListener来将ComponentContext放入上下文ServletContext

  ```java
  public class ComponentContextInitializerListener implements ServletContextListener {

    private ServletContext servletContext;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        this.servletContext = sce.getServletContext();
        ComponentContext context = new ComponentContext();
        context.init(servletContext);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    
    }

  }
  ```

  - 在模块user-web中添加依赖my-dependency-injection即可使用

## 作业内容2

> 完善 my-configuration 模块
>  Config 对象如何能被 my-web-mvc 使用

- 整体思路是在ServletContextConfigInitializer中将ConfigProviderResolver放入ServletContext中，然后在my-web-mvc的FrontControllerServlet中从ServletContext获取

  ```java
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();
        ServletContextConfigSource servletContextConfigSource = new ServletContextConfigSource(servletContext);
        // 获取当前 ClassLoader
        ClassLoader classLoader = servletContext.getClassLoader();
        ConfigProviderResolver configProviderResolver = ConfigProviderResolver.instance();
        ConfigBuilder configBuilder = configProviderResolver.getBuilder();
        // 配置 ClassLoader
        configBuilder.forClassLoader(classLoader);
        // 默认配置源（内建的，静态的）
        configBuilder.addDefaultSources();
        // 通过发现配置源（动态的）
        configBuilder.addDiscoveredConverters();
        // 增加扩展配置源（基于 Servlet 引擎）
        configBuilder.withSources(servletContextConfigSource);
        // 获取 Config
        Config config = configBuilder.build();
        // 注册 Config 关联到当前 ClassLoader
        configProviderResolver.registerConfig(config, classLoader);

        //将configProviderResolver注册到当前configProviderResolver，类似于ComponentContext#init中的做法
        servletContext.setAttribute(CONFIG_NAME, configProviderResolver);
    }
  ```

- 然后在init方法中获取

  ```java
    /**
     * 初始化 Servlet
     *
     * @param servletConfig
     */
    public void init(ServletConfig servletConfig) {
        try {
            super.init(servletConfig);

        } catch (ServletException e) {
            e.printStackTrace();
        }
        this.config = ((ConfigProviderResolver) getServletContext().getAttribute(CONFIG_NAME)).getConfig();
        initHandleMethods();
    }
  ```