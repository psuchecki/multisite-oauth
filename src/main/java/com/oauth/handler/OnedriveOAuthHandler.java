package com.oauth.handler;

import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.model.Verifier;
import com.github.scribejava.core.oauth.OAuthService;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.oauth.provider.OneDriveProvider;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class OnedriveOAuthHandler implements OAuthHandler {
    private static final String APP_NAME = "onedrive";
    public static final String FILE_DOWNLOAD_PATH =
            "https://api.onedrive.com/v1.0/drive/items/%s/content?download=true";
    public static final String FOLDER_DOWNLOAD_PATH =
            "https://api.onedrive.com/v1.0/drive/items/%s/children?select=id,folder,name";
    public static final String ROOT_FOLDER_DOWNLOAD_PATH =
            "https://api.onedrive.com/v1.0/drive/root/children?select=id,folder,name";

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
    public List<String> getSampleData(Token accessToken) throws IOException {
        List<String> downloadedFiles = Lists.newArrayList();
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        Client client = Client.create();
        WebResource webResource = client.resource(ROOT_FOLDER_DOWNLOAD_PATH);
        queryParams.add("access_token", accessToken.getToken());
        ClientResponse clientResponse =
                webResource.queryParams(queryParams).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        String rootFolderInfo = clientResponse.getEntity(String.class);
        handleFolder(client, queryParams, rootFolderInfo, downloadedFiles);

        return downloadedFiles;
    }

    private void handleFolder(Client client, MultivaluedMap<String, String> queryParams, String folderInfo,
                              List<String> downloadedFiles)
            throws IOException {
        JsonObject jsonObject = new JsonParser().parse(folderInfo).getAsJsonObject();
        JsonArray jsonArray = jsonObject.get("value").getAsJsonArray();
        for (JsonElement jsonElement : jsonArray) {
            JsonObject folderOrFile = jsonElement.getAsJsonObject();
            if (folderOrFile.get("folder") == null) {
                handleFile(client, queryParams, folderOrFile);
                downloadedFiles.add(folderOrFile.get("name").getAsString());
            } else {
                String folderId = folderOrFile.get("id").getAsString();
                WebResource webResource = client.resource(String.format(FOLDER_DOWNLOAD_PATH, folderId));
                ClientResponse clientResponse = webResource.queryParams(queryParams).accept(MediaType.APPLICATION_JSON)
                        .get(ClientResponse.class);
                String childFolderInfo = clientResponse.getEntity(String.class);
                handleFolder(client, queryParams, childFolderInfo, downloadedFiles);
            }
        }
    }

    private void handleFile(Client client, MultivaluedMap<String, String> queryParams, JsonObject fileInfo)
            throws IOException {
        String fileId = fileInfo.get("id").getAsString();
        WebResource webResource = client.resource(String.format(FILE_DOWNLOAD_PATH, fileId));
        File downloadedFile = webResource.queryParams(queryParams).accept(MediaType.APPLICATION_OCTET_STREAM)
                .get(ClientResponse.class).getEntity(File.class);

        String filePath = String.format("target/%s/%s-%s", APP_NAME, fileId, fileInfo.get("name").getAsString());
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        Files.deleteIfExists(path);
        FileUtils.moveFile(downloadedFile, new File(filePath));
    }
}
