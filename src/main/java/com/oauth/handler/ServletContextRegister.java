package com.oauth.handler;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.http.HttpServlet;

public class ServletContextRegister {
    private static final String SINGIN_PATH = "/%s-signin";
    private static final String CALLBACK_PATH = "/%s-callback";

    public static void registerServlets(ServletContextHandler contextHandler, HttpServlet signinServlet,
                                        HttpServlet callbackServlet, String appName) {
        contextHandler.addServlet(new ServletHolder(signinServlet), String.format(SINGIN_PATH, appName));
        contextHandler.addServlet(new ServletHolder(callbackServlet), String.format(CALLBACK_PATH, appName));
    }
}
