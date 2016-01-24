package com.oauth.client;

import com.github.scribejava.core.model.Token;

public interface FileDownloaderApi {
    Token downloadUserFiles(String rawResponse) throws Exception;
}
