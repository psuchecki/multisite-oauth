package com.oauth.handler;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.model.Verifier;
import com.github.scribejava.core.oauth.OAuthService;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.oauth.provider.DropBoxProvider;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

public class DropboxOAuthHandler implements OAuthHandler {
    private static final String APP_NAME = "dropbox";

    private OAuthService service;

    @Override
    public void registerServletHandler(ServletContextHandler contextHandler) {
        ServletContextRegister.registerServlets(contextHandler, new SigninServlet(), new CallbackServlet(), APP_NAME);
    }

    @Override
    public List<String> getSampleData(Token accessToken) throws Exception {
        List<String> downloadedFiles = Lists.newArrayList();
        DbxRequestConfig config = new DbxRequestConfig("JavaTutorial/1.0", Locale.getDefault().toString());
        DbxClient client = new DbxClient(config, accessToken.getToken());
        handleFolder(client, "/", downloadedFiles);

        return downloadedFiles;
    }

    private void handleFolder(DbxClient client, String folderPath, List<String> downloadedFiles)
            throws DbxException, IOException {
        DbxEntry.WithChildren listing = client.getMetadataWithChildren(folderPath);
        for (DbxEntry child : listing.children) {
            if (child.isFile()) {
                handleFile(client, child, downloadedFiles);
            } else if (child.isFolder()) {
                handleFolder(client, child.path, downloadedFiles);
            }
        }
    }

    private void handleFile(DbxClient client, DbxEntry file, List<String> downloadedFiles)
            throws IOException, DbxException {
        String filePath = String.format("target/%s%s", APP_NAME, file.path);
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        Files.deleteIfExists(path);
        try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
            DbxEntry.File downloadedFile = client.getFile(file.path, ((DbxEntry.File) file).rev, outputStream);
            downloadedFiles.add(downloadedFile.path);
        }
    }


    private class SigninServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            service = OAuthServiceProvider.getInstance(APP_NAME, DropBoxProvider.class);
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

            try {
                resp.getWriter().println(getSampleData(accessToken));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
