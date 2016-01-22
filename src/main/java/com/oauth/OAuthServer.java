package com.oauth;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class OAuthServer {
    private Server server = new Server(8080);

    public static void main(String[] args) throws Exception {
        new OAuthServer().startJetty();
    }

    public void startJetty() throws Exception {
        PropertiesHolder.initialize();
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        new EvernoteOAuthHandler().registerServletHandler(context);

        server.start();
        server.join();
    }
}
