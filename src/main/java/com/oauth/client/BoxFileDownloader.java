package com.oauth.client;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.oauth.OAuthService;
import com.google.common.collect.Lists;
import com.oauth.handler.BoxOAuthHandler;
import com.oauth.provider.OAuthServiceProvider;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.oauth.handler.BoxOAuthHandler.APP_NAME;
import static com.oauth.handler.BoxOAuthHandler.BOX_PROVIDER;

public class BoxFileDownloader implements FileDownloaderApi {

    @Override
    public Token downloadUserFiles(String rawResponse) throws IOException {
        Token accessToken = BOX_PROVIDER.getAccessTokenExtractor().extract(rawResponse);
        OAuthService service = OAuthServiceProvider.getInstance(BoxOAuthHandler.APP_NAME, BOX_PROVIDER.getClass());

        Token refreshedAccessToken = new TokenRefresher().refreshAccessToken(accessToken, BOX_PROVIDER, service);
        List<String> downloadedFiles = Lists.newArrayList();
        BoxAPIConnection boxClient = new BoxAPIConnection(refreshedAccessToken.getToken());
        BoxFolder rootFolder = BoxFolder.getRootFolder(boxClient);

        visitFolder(rootFolder, boxClient, downloadedFiles);

        return refreshedAccessToken;
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
}
