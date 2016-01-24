package com.oauth.handler;

import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.model.Verifier;
import com.github.scribejava.core.oauth.OAuthService;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.oauth.provider.SlackProvider;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import flowctrl.integration.slack.SlackClientFactory;
import flowctrl.integration.slack.type.File;
import flowctrl.integration.slack.type.FileList;
import flowctrl.integration.slack.webapi.SlackWebApiClient;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class SlackOAuthHandler implements OAuthHandler {
    public static final String APP_NAME = "slack";
    public static final SlackProvider SLACK_PROVIDER = new SlackProvider();

    private OAuthService service;

    @Override
    public void registerServletHandler(ServletContextHandler contextHandler) {
        ServletContextRegister.registerServlets(contextHandler, new SigninServlet(), new CallbackServlet(), APP_NAME);
    }

    @Override
    public Token downloadUserFiles(String rawResponse) throws IOException {
        Token accessToken = SLACK_PROVIDER.getAccessTokenExtractor().extract(rawResponse);
        List<String> downloadedFiles = Lists.newArrayList();

        SlackWebApiClient client = SlackClientFactory.createWebApiClient(accessToken.getToken());
        FileList fileList = client.getFileList();
        for (File file : fileList.getFiles()) {
            downloadFile(file, null);
            downloadedFiles.add(file.getName());
        }

        return accessToken;
    }

    private void downloadFile(File file, String token) throws IOException {
        String url_private = file.getUrl_private_download();
        Client restClient = Client.create();
        WebResource webResource = restClient.resource(url_private);
        java.io.File downloadedFile = webResource.header("Authorization", "Bearer " + token).post(ClientResponse.class)
                .getEntity(java.io.File.class);

        String filePath = String.format("target/%s/%s-%s", APP_NAME, file.getId(), file.getName());
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        Files.deleteIfExists(path);
        FileUtils.moveFile(downloadedFile, new java.io.File(filePath));
    }


    private class SigninServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            service = OAuthServiceProvider.getInstance(APP_NAME, SLACK_PROVIDER.getClass());
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

            resp.getWriter().println(downloadUserFiles(accessToken.getRawResponse()));
        }

    }
}
