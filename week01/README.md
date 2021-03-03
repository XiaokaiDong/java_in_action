# 小马哥JAVA实战营第一周作业

基于小马哥的课程案例

## 作业内容1

```
通过自研 Web MVC 框架实现（可以自己实现）一个用户注册，forward 到一个成功的页面（JSP 用法）

    /register
    
通过 Controller -> Service -> Repository 实现（数据库实现）
```

- 添加RegisterController，处理/user/register注册用户请求，注册成功返回registerSuccess.jsp，注册失败返回registerFailing.jsp。

  ```java
    @Override
    @POST
    @Path("/register")
    public String execute(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        logger.log(Level.INFO,"收到请求/user/register...");

        User user = new User();
        user.setName(request.getParameter("userName"));
        user.setEmail(request.getParameter("email"));
        user.setPassword(request.getParameter("password"));
        user.setPhoneNumber(request.getParameter("phoneNumber"));

        boolean result = userService.register(user);

        if(result) {
            return "registerSuccess.jsp";
        } else {
            return "registerFailing.jsp";
        }
    }
  ```

- 注册时使用UserService完成实际的注册任务，UserService在构造RegisterController时设置

  ```java
    private UserService userService;

    public RegisterController() {
        Connection connection = getConnection(2);

        DBConnectionManager connectionManager = new DBConnectionManager();
        connectionManager.setConnection(connection);

        DatabaseUserRepository userRepository = new DatabaseUserRepository(connectionManager);


        this.userService = new DatabaseUserService(userRepository);
    }
  ```

- UserService注册用户就是在数据库中增加相应的记录

  ```java
    @Override
    public boolean register(User user) {
        logger.log(Level.INFO, "开始注册用户...");
        return userRepository.save(user);
    }
  ```

- UserRepository#save的数据库实现如下

  ```java
    @Override
    public boolean save(User user) {
        logger.log(Level.INFO, "开始保存用户：" + user);

        int result = executeUpdate(INSERT_USER_DML_SQL, COMMON_EXCEPTION_HANDLER,
                user.getName(), user.getPassword(),
                user.getEmail(), user.getPhoneNumber());

        return result >= 0;
    }
  ```

  - executeUpdate的实现如下

  ```java
  protected int executeUpdate(String sql, Consumer<Throwable> exceptionHandler, Object... args) {
        int numOfRows = 0;

        Connection connection = getConnection();
        try {
            PreparedStatement preparedStatement = getPreparedStatement(sql, connection, args);
            numOfRows = preparedStatement.executeUpdate();
            logger.log(Level.INFO, "preparedStatement.executeUpdate执行完毕， numOfRows: " + numOfRows);

        } catch (Throwable e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "executeUpdate发生异常： " + e.getMessage());
            exceptionHandler.accept(e);
            numOfRows = -1;
        }

        return numOfRows;
    }

  ```

  - getPreparedStatement方法就是小马哥老师在课上代码抽取到一个方法中而已

  ```java
    /**
     *
     * @param sql  待执行的SQL语句
     * @param connection  数据库连接
     * @param args  SQL语句的参数
     * @return 准备好的PreparedStatement
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private PreparedStatement getPreparedStatement(String sql, Connection connection, Object[] args) throws SQLException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            Class argType = arg.getClass();


            Class wrapperType = wrapperToPrimitive(argType);


            if (wrapperType == null) {
                wrapperType = argType;
            }

            logger.log(Level.INFO, "===========");
            logger.log(Level.INFO, "args" + i);
            logger.log(Level.INFO, "argType: " + argType);
            logger.log(Level.INFO, "wrapperType: " + wrapperType);

            // Boolean -> boolean
            String methodName = preparedStatementMethodMappings.get(argType);
            logger.log(Level.INFO, "methodName: " + methodName);
            Method method = PreparedStatement.class.getMethod(methodName, int.class, wrapperType);
            logger.log(Level.INFO, "method: " + method);
            logger.log(Level.INFO, "arg: " + arg);
            method.invoke(preparedStatement, i + 1, arg);

            logger.log(Level.INFO, "===========");
        }
        return preparedStatement;
    }
  ```

## 作业内容2

```
JDNI 的方式获取数据库源（DataSource），在获取 Connection
```

- 获取数据连接可以通过SPI方式，也可以通过JNDI方式，封装在RegisterController#getConnection方法中

  ```java
    /**
     *
     * @param method 获取数据库连接的方法：1, SPI; 2, JNDI
     * @return
     */
    private Connection getConnection(int method) {
        Connection connection = null;
        if (method == 1) {
            String databaseURL = "jdbc:derby:/derby/db/user-platform;create=true";
            try {
                connection = DriverManager.getConnection(databaseURL);
                logger.log(Level.INFO, "使用SPI方式得到连接" + connection);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        } else if(method == 2) {
            try {
                Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
                Context initContext = new InitialContext();
                //Context envContext = (Context) initContext.lookup("java:/comp/env/jdbc/UserPlatformDB");
                DataSource ds = (DataSource) initContext.lookup("java:/comp/env/jdbc/UserPlatformDB");
                //DataSource ds = (DataSource) envContext.lookup("jdbc/UserPlatformDB");
                connection = ds.getConnection();
                logger.log(Level.INFO, "使用JNDI方式得到连接" + connection);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return connection;
    }
  ```

  ```
  目前JNDI实现还有问题，DataSource ds = (DataSource) initContext.lookup("java:/comp/env/jdbc/UserPlatformDB")抛出javax.naming.NoInitialContextException异常，希望老师可以帮助排查问题。
  ```