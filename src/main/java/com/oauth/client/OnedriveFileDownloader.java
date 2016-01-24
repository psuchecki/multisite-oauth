package com.oauth.client;

import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.oauth.OAuthService;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.oauth.handler.OAuthServiceProvider;
import com.oauth.handler.OnedriveOAuthHandler;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.io.FileUtils;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.oauth.handler.OnedriveOAuthHandler.*;

public class OnedriveFileDownloader implements FileDownloaderApi {
    @Override
    public Token downloadUserFiles(String rawResponse) throws IOException {
        Token accessToken = ONE_DRIVE_PROVIDER.getAccessTokenExtractor().extract(rawResponse);
        OAuthService service = OAuthServiceProvider.getInstance(OnedriveOAuthHandler.APP_NAME, ONE_DRIVE_PROVIDER.getClass());
        Token refreshedToken = new TokenRefresher().refreshAccessToken(accessToken, ONE_DRIVE_PROVIDER, service);

        List<String> downloadedFiles = Lists.newArrayList();
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        Client client = Client.create();
        WebResource webResource = client.resource(ROOT_FOLDER_DOWNLOAD_PATH);
        queryParams.add("access_token", refreshedToken.getToken());
        ClientResponse clientResponse =
                webResource.queryParams(queryParams).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        String rootFolderInfo = clientResponse.getEntity(String.class);
        visitFolder(client, queryParams, rootFolderInfo, downloadedFiles);

        return refreshedToken;
    }

    private void visitFolder(Client client, MultivaluedMap<String, String> queryParams, String folderInfo,
                             List<String> downloadedFiles)
            throws IOException {
        JsonObject jsonObject = new JsonParser().parse(folderInfo).getAsJsonObject();
        JsonArray jsonArray = jsonObject.get("value").getAsJsonArray();
        for (JsonElement jsonElement : jsonArray) {
            JsonObject folderOrFile = jsonElement.getAsJsonObject();
            if (folderOrFile.get("folder") == null) {
                downloadFile(client, queryParams, folderOrFile);
                downloadedFiles.add(folderOrFile.get("name").getAsString());
            } else {
                String folderId = folderOrFile.get("id").getAsString();
                WebResource webResource = client.resource(String.format(FOLDER_DOWNLOAD_PATH, folderId));
                ClientResponse clientResponse = webResource.queryParams(queryParams).accept(MediaType.APPLICATION_JSON)
                        .get(ClientResponse.class);
                String childFolderInfo = clientResponse.getEntity(String.class);
                visitFolder(client, queryParams, childFolderInfo, downloadedFiles);
            }
        }
    }

    private void downloadFile(Client client, MultivaluedMap<String, String> queryParams, JsonObject fileInfo)
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
