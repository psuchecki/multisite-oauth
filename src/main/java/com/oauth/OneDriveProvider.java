package com.oauth;

import com.github.scribejava.apis.google.GoogleJsonTokenExtractor;
import com.github.scribejava.apis.service.GoogleOAuthServiceImpl;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.AccessTokenExtractor;
import com.github.scribejava.core.model.OAuthConfig;
import com.github.scribejava.core.oauth.OAuthService;
import com.github.scribejava.core.utils.OAuthEncoder;

public class OneDriveProvider extends DefaultApi20 {

    public static final String AUTHORIZE_URL =
            "https://login.live.com/oauth20_authorize.srf?client_id=%s&scope=%s&response_type=code&redirect_uri=%s";
    public static final String ACCESS_TOKEN_ENDPOINT = "https://login.live.com/oauth20_token.srf";

    @Override
    public String getAccessTokenEndpoint() {
        return ACCESS_TOKEN_ENDPOINT;
    }

    @Override
    public String getAuthorizationUrl(OAuthConfig config) {
        return String.format(AUTHORIZE_URL, new Object[]{config.getApiKey(), OAuthEncoder.encode(config.getScope()),
                OAuthEncoder.encode(config.getCallback())});
    }

    public AccessTokenExtractor getAccessTokenExtractor() {
        return new GoogleJsonTokenExtractor();
    }

    public OAuthService createService(OAuthConfig config) {
        return new GoogleOAuthServiceImpl(this, config);
    }
}
