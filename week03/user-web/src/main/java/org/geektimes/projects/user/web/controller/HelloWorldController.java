package org.geektimes.projects.user.web.controller;

import org.geektimes.web.mvc.controller.PageController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 输出 “Hello,World” Controller
 */
@Path("/hello")
public class HelloWorldController implements PageController {

    private static Logger logger = Logger.getLogger(HelloWorldController.class.getName());

    @GET
    //@POST
    @Path("/world") // /hello/world -> HelloWorldController
    public String execute(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        logger.log(Level.INFO, "收到/hello/world请求");
        return "index.jsp";
    }
}
