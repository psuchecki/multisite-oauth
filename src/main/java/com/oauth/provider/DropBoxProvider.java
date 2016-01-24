package com.oauth.provider;

import com.github.scribejava.apis.service.GoogleOAuthServiceImpl;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.AccessTokenExtractor;
import com.github.scribejava.core.extractors.JsonTokenExtractor;
import com.github.scribejava.core.model.OAuthConfig;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuthService;
import com.github.scribejava.core.utils.OAuthEncoder;

public class DropBoxProvider extends DefaultApi20 {
    public static final String AUTHORIZE_URL =
            "https://www.dropbox.com/1/oauth2/authorize?client_id=%s&response_type=code&redirect_uri=%s";
    public static final String ACCESS_TOKEN_ENDPOINT = "https://api.dropbox.com/1/oauth2/token";

    @Override
    public String getAccessTokenEndpoint() {
        return ACCESS_TOKEN_ENDPOINT;
    }

    @Override
    public String getAuthorizationUrl(OAuthConfig config) {
        return String
                .format(AUTHORIZE_URL, new Object[]{config.getApiKey(), OAuthEncoder.encode(config.getCallback())});
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
