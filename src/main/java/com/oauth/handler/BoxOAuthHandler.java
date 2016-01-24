package com.oauth.handler;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFile;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class BoxOAuthHandler implements OAuthHandler {
    private static final String APP_NAME = "box";

    private OAuthService service;

    @Override
    public void registerServletHandler(ServletContextHandler contextHandler) {
        ServletContextRegister.registerServlets(contextHandler, new SigninServlet(), new CallbackServlet(), APP_NAME);
    }

    @Override
    public List<String> getSampleData(Token accessToken) throws IOException {
        List<String> downloadedFiles = Lists.newArrayList();
        BoxAPIConnection boxClient = new BoxAPIConnection(accessToken.getToken());
        BoxFolder rootFolder = BoxFolder.getRootFolder(boxClient);

        visitFolder(rootFolder, boxClient, downloadedFiles);

        return downloadedFiles;
    }

    private void visitFolder(BoxFolder rootFolder, BoxAPIConnection boxClient, List<String> downloadedFiles) throws IOException {
        for (BoxItem.Info itemInfo : rootFolder) {
            if (itemInfo instanceof BoxFile.Info) {
                BoxFile.Info fileInfo = (BoxFile.Info) itemInfo;
                downloadFile(boxClient, fileInfo);
                downloadedFiles.add(fileInfo.getName());
            } else if (itemInfo instanceof BoxFolder.Info) {
                BoxFolder.Info folderInfo = (BoxFolder.Info) itemInfo;
                BoxFolder folder = new BoxFolder(boxClient, folderInfo.getID());
                visitFolder(folder, boxClient, downloadedFiles);
            }
        }
    }

    private void downloadFile(BoxAPIConnection boxClient, BoxFile.Info fileInfo) throws IOException {
        String filePath = String.format("target/%s/%s-%s", APP_NAME, fileInfo.getID(), fileInfo.getName());
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        Files.deleteIfExists(path);
        BoxFile file = new BoxFile(boxClient, fileInfo.getID());
        try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
            file.download(outputStream);
        }
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

            resp.getWriter().println(getSampleData(accessToken));
        }

    }
}
