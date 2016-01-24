package com.oauth.provider;

import com.github.scribejava.apis.service.GoogleOAuthServiceImpl;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.AccessTokenExtractor;
import com.github.scribejava.core.extractors.JsonTokenExtractor;
import com.github.scribejava.core.model.OAuthConfig;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuthService;
import com.github.scribejava.core.utils.OAuthEncoder;
import com.oauth.PropertiesHolder;

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
        return String.format(AUTHORIZE_URL,
                new Object[]{config.getApiKey(), PropertiesHolder.getProperty("onedrive.scope"),
                        OAuthEncoder.encode(config.getCallback())});
    }

    public Verb getAccessTokenVerb() {
        return Verb.POST;
    }

    public AccessTokenExtractor getAccessTokenExtractor() {
        return new JsonTokenExtractor();
    }

    public OAuthService createService(OAuthConfig config) {
        return new GoogleOAuthServiceImpl(this, config);
    }
}
