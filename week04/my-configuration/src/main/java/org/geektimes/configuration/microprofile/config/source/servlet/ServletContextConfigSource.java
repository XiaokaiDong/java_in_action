package org.geektimes.configuration.microprofile.config.source.servlet;

import org.geektimes.configuration.microprofile.config.source.MapBasedConfigSource;

import javax.servlet.ServletContext;
import java.util.Enumeration;
import java.util.Map;

public class ServletContextConfigSource extends MapBasedConfigSource {

    private final ServletContext servletContext;

    public ServletContextConfigSource(ServletContext servletContext) {
        super("ServletContext Init Parameters", 500);
        this.servletContext = servletContext;
        //此时servletContext被初始化，才可以初始化父类的source成员
        this.source = getProperties();
    }

    @Override
    protected void prepareConfigData(Map configData) throws Throwable {
        if (servletContext == null) {
            return;
        }
        Enumeration<String> parameterNames = servletContext.getInitParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();
            configData.put(parameterName, servletContext.getInitParameter(parameterName));
        }
    }
}
