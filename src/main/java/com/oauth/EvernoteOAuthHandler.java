package com.oauth;

import com.evernote.auth.EvernoteAuth;
import com.evernote.auth.EvernoteService;
import com.evernote.clients.ClientFactory;
import com.evernote.clients.NoteStoreClient;
import com.evernote.edam.type.Notebook;
import com.github.scribejava.apis.EvernoteApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.model.Verifier;
import com.github.scribejava.core.oauth.OAuthService;
import com.google.common.base.Strings;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class EvernoteOAuthHandler implements OAuthHandler {

    private static final String CONSUMER_KEY = "psuchecki";
    private static final String CONSUMER_SECRET = "ef21a681f9e8465e";
    private static final String CALLBACK = "http://localhost:8080/evernote-callback";
    private static final String VERIFIER_PARAM = "oauth_verifier";

    private OAuthService service;
    private Token requestToken;

    @Override
    public void registerServletHandler(ServletContextHandler contextHandler) {
        contextHandler.addServlet(new ServletHolder(new SigninServlet()), "/evernote-singin");
        contextHandler.addServlet(new ServletHolder(new CallbackServlet()), "/evernote-callback");
    }


    private class SigninServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            service = new ServiceBuilder().provider(EvernoteApi.Sandbox.class).apiKey(CONSUMER_KEY)
                    .apiSecret(CONSUMER_SECRET).callback(CALLBACK).build();
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

            printSampleData(accessToken, resp);
        }

        private void printSampleData(Token accessToken, HttpServletResponse resp) throws IOException {
            try {
                List<Notebook> notebooks = getSampleData(accessToken);
                for (Notebook notebook : notebooks) {
                    resp.getWriter().println(notebook);
                }
            } catch (Exception e) {
                resp.getWriter().println(e);
            }
        }
    }

    @Override
    public List<Notebook> getSampleData(Token accessToken) throws Exception {
        EvernoteAuth evernoteAuth = new EvernoteAuth(EvernoteService.SANDBOX, accessToken.getToken());
        NoteStoreClient noteStoreClient = new ClientFactory(evernoteAuth).createNoteStoreClient();

        return noteStoreClient.listNotebooks();
    }
}
