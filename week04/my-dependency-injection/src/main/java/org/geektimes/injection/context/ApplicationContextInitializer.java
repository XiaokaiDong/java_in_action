package org.geektimes.injection.context;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.Set;

import org.geektimes.injection.listener.ComponentContextInitializerListener;

public class ApplicationContextInitializer implements ServletContainerInitializer {
    @Override
    public void onStartup(Set<Class<?>> c, ServletContext servletContext) throws ServletException {
        servletContext.addListener(ComponentContextInitializerListener.class);
    }
}
