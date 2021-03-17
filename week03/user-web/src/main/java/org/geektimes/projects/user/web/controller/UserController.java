package org.geektimes.projects.user.web.controller;

import org.geektimes.web.mvc.controller.PageController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/hello")
public class UserController implements PageController {

    private static Logger logger = Logger.getLogger(UserController.class.getName());

    @GET
    //@POST
    @Path("/user") // /hello/world -> HelloWorldController
    public String execute(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        logger.log(Level.INFO, "收到/hello/user请求");
        return "login-form.jsp";
    }
}
