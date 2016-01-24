package com.oauth.handler;

import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.model.Verifier;
import com.github.scribejava.core.oauth.OAuthService;
import com.google.common.base.Strings;
import com.oauth.client.OnedriveFileDownloader;
import com.oauth.provider.OAuthServiceProvider;
import com.oauth.provider.OneDriveProvider;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class OnedriveOAuthHandler implements OAuthHandler {
    public static final String APP_NAME = "onedrive";
    public static final String FILE_DOWNLOAD_PATH =
            "https://api.onedrive.com/v1.0/drive/items/%s/content?download=true";
    public static final String FOLDER_DOWNLOAD_PATH =
            "https://api.onedrive.com/v1.0/drive/items/%s/children?select=id,folder,name";
    public static final String ROOT_FOLDER_DOWNLOAD_PATH =
            "https://api.onedrive.com/v1.0/drive/root/children?select=id,folder,name";
    public static final OneDriveProvider ONE_DRIVE_PROVIDER = new OneDriveProvider();

    private OAuthService service;

    @Override
    public void registerServletHandler(ServletContextHandler contextHandler) {
        ServletContextRegister.registerServlets(contextHandler, new SigninServlet(), new CallbackServlet(), APP_NAME);
    }

    private class SigninServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            service = OAuthServiceProvider.getInstance(APP_NAME, ONE_DRIVE_PROVIDER.getClass());
            String authorizationUrl = service.getAuthorizationUrl(EMPTY_TOKEN);

            resp.sendRedirect(authorizationUrl);
        }
    }

    private class CallbackServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String verifierParam = req.getParameter(CODE_PARAM);
            if (Strings.isNullOrEmpty(verifierParam)) {
                resp.getWriter().println("Failed to login to user account");
                return;
            }

            Verifier verifier = new Verifier(verifierParam);
            Token accessToken = service.getAccessToken(EMPTY_TOKEN, verifier);

            Token token = new OnedriveFileDownloader().downloadUserFiles(accessToken.getRawResponse());
            resp.getWriter().println(token);
        }
    }

}
