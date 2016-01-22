package com.oauth;

import com.evernote.edam.type.Notebook;
import com.github.scribejava.core.model.Token;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface OAuthHandler {
    void registerServletHandler(ServletContextHandler contextHandler);

    List<?> getSampleData(Token accessToken) throws Exception;
}
