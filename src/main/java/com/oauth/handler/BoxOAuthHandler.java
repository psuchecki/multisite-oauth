package com.oauth.handler;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.model.Verifier;
import com.github.scribejava.core.oauth.OAuthService;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.oauth.provider.BoxProvider;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class BoxOAuthHandler implements OAuthHandler {
    private static final Token EMPTY_TOKEN = null;
    private static final String CODE_PARAM = "code";
    public static final String APP_NAME = "box";

    private OAuthService service;

    @Override
    public void registerServletHandler(ServletContextHandler contextHandler) {
        ServletContextRegister.registerServlets(contextHandler, new SigninServlet(), new CallbackServlet(), APP_NAME);
    }

    @Override
    public List<BoxItem.Info> getSampleData(Token accessToken) {
        BoxAPIConnection api = new BoxAPIConnection(accessToken.getToken());
        BoxFolder rootFolder = BoxFolder.getRootFolder(api);

        return Lists.newArrayList(rootFolder);
    }


    private class SigninServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            service = OAuthServiceProvider.getInstance(APP_NAME, BoxProvider.class);
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

            printSampleData(resp, accessToken);
        }

        private void printSampleData(HttpServletResponse resp, Token accessToken) throws IOException {
            List<BoxItem.Info> itemInfos = getSampleData(accessToken);
            for (BoxItem.Info itemInfo : itemInfos) {
                resp.getWriter().println(String.format("[%s] %s\n", itemInfo.getID(), itemInfo.getName()));
            }
        }
    }
}
