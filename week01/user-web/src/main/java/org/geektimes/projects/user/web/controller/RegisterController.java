package org.geektimes.projects.user.web.controller;

import org.geektimes.projects.user.domain.User;
import org.geektimes.projects.user.repository.DatabaseUserRepository;
import org.geektimes.projects.user.service.DatabaseUserService;
import org.geektimes.projects.user.service.UserService;
import org.geektimes.projects.user.sql.DBConnectionManager;
import org.geektimes.web.mvc.controller.PageController;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/user")
public class RegisterController implements PageController {
    private static Logger logger = Logger.getLogger(RegisterController.class.getName());

    private UserService userService;

    public RegisterController() {
        Connection connection = getConnection(2);

        DBConnectionManager connectionManager = new DBConnectionManager();
        connectionManager.setConnection(connection);

//        if (connection == null) {
//            throw new Exception("获取数据库连接失败");
//        }

        DatabaseUserRepository userRepository = new DatabaseUserRepository(connectionManager);


        this.userService = new DatabaseUserService(userRepository);
    }

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
}
