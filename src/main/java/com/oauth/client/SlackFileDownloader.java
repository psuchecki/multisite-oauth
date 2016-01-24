package com.oauth.client;

import com.github.scribejava.core.model.Token;
import com.google.common.collect.Lists;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import flowctrl.integration.slack.SlackClientFactory;
import flowctrl.integration.slack.type.File;
import flowctrl.integration.slack.type.FileList;
import flowctrl.integration.slack.webapi.SlackWebApiClient;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.oauth.handler.SlackOAuthHandler.APP_NAME;
import static com.oauth.handler.SlackOAuthHandler.SLACK_PROVIDER;

public class SlackFileDownloader implements FileDownloaderApi {
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
}
