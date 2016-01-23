package com.oauth.handler;

import com.github.scribejava.core.model.Token;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.util.List;

public interface OAuthHandler {
    void registerServletHandler(ServletContextHandler contextHandler);
    List<?> getSampleData(Token accessToken) throws Exception;
}
