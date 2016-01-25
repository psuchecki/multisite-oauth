package com.oauth.handler;

import com.github.scribejava.apis.EvernoteApi;
import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.model.Verifier;
import com.github.scribejava.core.oauth.OAuthService;
import com.google.common.base.Strings;
import com.oauth.PropertiesHolder;
import com.oauth.client.EvernoteFileDownloader;
import com.oauth.provider.OAuthServiceProvider;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class EvernoteOAuthHandler implements OAuthHandler {
    private static final String VERIFIER_PARAM = "oauth_verifier";
    public static final String APP_NAME = "evernote";
    public static final EvernoteApi EVERNOTE_API = PropertiesHolder.useEvernoteSandbox() ? new EvernoteApi.Sandbox() : new EvernoteApi();

    private OAuthService service;
    private Token requestToken;

    @Override
    public void registerServletHandler(ServletContextHandler contextHandler) {
        ServletContextRegister.registerServlets(contextHandler, new SigninServlet(), new CallbackServlet(), APP_NAME);
    }


    private class SigninServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            service = OAuthServiceProvider.getInstance(APP_NAME, EVERNOTE_API.getClass());
            requestToken = service.getRequestToken();
            String authorizationUrl = service.getAuthorizationUrl(requestToken);

            resp.sendRedirect(authorizationUrl);
        }
    }

    private class CallbackServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String verifierParam = req.getParameter(VERIFIER_PARAM);
            if (Strings.isNullOrEmpty(verifierParam)) {
                resp.getWriter().println("Failed to login to user account");
                return;
            }

            Verifier verifier = new Verifier(verifierParam);
            Token accessToken = service.getAccessToken(requestToken, verifier);

            try {
                Token token = new EvernoteFileDownloader().downloadUserFiles(accessToken.getRawResponse());
                resp.getWriter().println(token);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
