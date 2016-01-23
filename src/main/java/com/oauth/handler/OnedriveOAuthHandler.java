package com.oauth.handler;

import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.model.Verifier;
import com.github.scribejava.core.oauth.OAuthService;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.oauth.provider.OneDriveProvider;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.List;

public class OnedriveOAuthHandler implements OAuthHandler {
    private static final Token EMPTY_TOKEN = null;
    private static final String CODE_PARAM = "code";
    public static final String APP_NAME = "onedrive";

    private OAuthService service;

    @Override
    public void registerServletHandler(ServletContextHandler contextHandler) {
        ServletContextRegister.registerServlets(contextHandler, new SigninServlet(), new CallbackServlet(), APP_NAME);
    }

    private class SigninServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            service = OAuthServiceProvider.getInstance(APP_NAME, OneDriveProvider.class);
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

            resp.getWriter().println(getSampleData(accessToken));
        }
    }

    @Override
    public List<String> getSampleData(Token accessToken) {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        Client client = Client.create();
        WebResource webResource = client.resource("https://api.onedrive.com/v1.0/drive");
        queryParams.add("access_token", accessToken.getToken());
        ClientResponse clientResponse =
                webResource.queryParams(queryParams).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        String response = clientResponse.getEntity(String.class);

        return Lists.newArrayList(response);
    }
}
