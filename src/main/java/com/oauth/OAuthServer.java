package com.oauth;

import com.google.common.collect.Lists;
import com.oauth.handler.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.util.List;

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

        List<OAuthHandler> oAuthHandlers =
                Lists.newArrayList(new BoxOAuthHandler(), new OnedriveOAuthHandler(), new EvernoteOAuthHandler(),
                        new SlackOAuthHandler());

        oAuthHandlers.stream().forEach(oAuthHandler -> oAuthHandler.registerServletHandler(context));

        server.start();
        server.join();
    }
}
