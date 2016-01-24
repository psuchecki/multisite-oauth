package com.oauth.client;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.github.scribejava.core.model.Token;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.oauth.handler.DropboxOAuthHandler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

import static com.oauth.handler.DropboxOAuthHandler.DROP_BOX_PROVIDER;

public class DropboxFileDownloader implements FileDownloaderApi {
    @Override
    public Token downloadUserFiles(String rawResponse) throws Exception {
        Token accessToken = DROP_BOX_PROVIDER.getAccessTokenExtractor().extract(rawResponse);
        DbxRequestConfig config = new DbxRequestConfig("JavaTutorial/1.0", Locale.getDefault().toString());
        DbxClient client = new DbxClient(config, accessToken.getToken());

        List<String> downloadedFiles = Lists.newArrayList();
        JsonObject rawResponseJson = new JsonParser().parse(accessToken.getRawResponse()).getAsJsonObject();
        String clientUid = rawResponseJson.get("uid").getAsString(); //needed to not overwrite if clients have same files
        visitFolder(client, "/", clientUid, downloadedFiles);

        return accessToken;
    }

    private void visitFolder(DbxClient client, String folderPath, String clientUid, List<String> downloadedFiles)
            throws DbxException, IOException {
        DbxEntry.WithChildren listing = client.getMetadataWithChildren(folderPath);
        for (DbxEntry child : listing.children) {
            if (child.isFile()) {
                downloadFile(client, child, clientUid, downloadedFiles);
            } else if (child.isFolder()) {
                visitFolder(client, child.path, clientUid, downloadedFiles);
            }
        }
    }

    private void downloadFile(DbxClient client, DbxEntry file, String clientUid, List<String> downloadedFiles)
            throws IOException, DbxException {
        String filePath = String.format("target/%s/%s%s", DropboxOAuthHandler.APP_NAME, clientUid, file.path);
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        Files.deleteIfExists(path);
        try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
            DbxEntry.File downloadedFile = client.getFile(file.path, ((DbxEntry.File) file).rev, outputStream);
            downloadedFiles.add(downloadedFile.path);
        }
    }
}
