# 小马哥JAVA实战营第二周作业

基于小马哥的课程案例

## 作业内容

```
通过课堂上的简易版依赖注入和依赖查找，实现用户注册功能

- 通过 UserService 实现用户注册
- 注册用户需要校验
  - Id：必须大于 0 的整数
  - 密码：6-32 位
  - 电话号码：采用中国大陆方式（11 位校验）
```

- 首先实现控制器中Service的注入

  >至于Service及其之后的层的自动注入小马哥已经使用JNDI实现了

  因为context.xml中声明的“组件”已经在ComponentContextInitializerListener的初始化中存在于组件上下文ComponentContext中了，所以可以在FrontControllerServlet的初始化代码中直接使用。

  实现过程借鉴了ComponentContext#injectComponents，位于FrontControllerServlet#initHandleMethods的后半部分。

  >主要是利用了ComponentContextInitializerListener初始化时将全局唯一的ComponentContext实例放入了ServletContext这一事实，在FrontControllerServlet初始化时将ComponentContext实例从上下文中取出，并利用反射的方式调用ComponentContext#getComponent自动注入控制器的@Resource属性。

  ```java
  //开始注入。此时ServletContextListener#contextInitialized已经执行完毕，
  //所以所需的BEAN已经位于容器上下文中了，可以直接注入。
  //仿照org.geektimes.context.ComponentContext.injectComponents
  Stream.of(controllerClass.getDeclaredFields())
          .filter(field -> {
              int mods = field.getModifiers();
              return !Modifier.isStatic(mods) &&
                      field.isAnnotationPresent(Resource.class);
          }).forEach(field -> {
      Resource resource = field.getAnnotation(Resource.class);
      String resourceName = resource.name();
      Object componentContext = getServletContext().getAttribute("org.geektimes.context.ComponentContext");
      //利用反射调用方法ComponentContext#getComponent
      Class<?> componentContextClass = componentContext.getClass();
      Method[] componentContextPublicMethods = componentContextClass.getMethods();
      // 利用getComponent注入@Resource属性
      for (Method method : componentContextPublicMethods) {
          if (method.getName().equals("getComponent")) {
              try {
              Object injectedObject = method.invoke(componentContext, resourceName);
              field.setAccessible(true);
              // 注入目标对象
              field.set(controller, injectedObject);
              } catch (IllegalAccessException | InvocationTargetException e) {
              }
          }

      }

  });
  ```

  >注意: 需要在FrontControllerServlet#init方法中调用父类的init方法，否则会取不到ServletContext

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
        initHandleMethods();
    }
  ```
- 实现用户注册

  将UserService注入控制器后，就可以利用小马哥已经实现的代码实现注册功能了。

  ```java
  @Override
    // 默认需要事务
    @LocalTransactional
    public boolean register(User user) {
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            // before process

            transaction.begin();

            // 主调用
            entityManager.persist(user);

            // 调用其他方法方法
            update(user); // 涉及事务

            // after process
            transaction.commit();
            logger.log(Level.INFO, "用户注册成功");
        }catch (Exception e) {
            logger.log(Level.INFO, "用户注册失败，准备回滚: " + e.getMessage());
            transaction.rollback();
        }

        return false;
    }
  ```

- 用户校验

  使用小马哥课上代码就可以实现了，主要是利用Java Bean Validation的注解

  ```java
  @Entity
  @Table(name = "users")
  public class User implements Serializable {

      @Id
      @GeneratedValue(strategy = AUTO)
      @Min(1)
      private Long id;

      @Column
      private String name;

      @Column
      @Size(min = 6, max = 32)
      private String password;

      @Column
      private String email;

      @Column
      @Pattern(regexp = "^1\\d(10)")
      private String phoneNumber;
  }
  ```

