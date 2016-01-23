package com.oauth.handler;

import com.github.scribejava.core.model.Token;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.util.List;

public interface OAuthHandler {
    Token EMPTY_TOKEN = null;
    String CODE_PARAM = "code";

    void registerServletHandler(ServletContextHandler contextHandler);
    List<?> getSampleData(Token accessToken) throws Exception;
}
