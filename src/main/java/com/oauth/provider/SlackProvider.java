package com.oauth.provider;

import com.github.scribejava.apis.google.GoogleJsonTokenExtractor;
import com.github.scribejava.apis.service.GoogleOAuthServiceImpl;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.AccessTokenExtractor;
import com.github.scribejava.core.model.OAuthConfig;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuthService;
import com.github.scribejava.core.utils.OAuthEncoder;
import com.oauth.PropertiesHolder;

public class SlackProvider extends DefaultApi20 {
    public static final String AUTHORIZE_URL =
            "https://slack.com/oauth/authorize?client_id=%s&scope=%s&redirect_uri=%s";
    public static final String ACCESS_TOKEN_ENDPOINT = "https://slack.com/api/oauth.access";

    @Override
    public String getAccessTokenEndpoint() {
        return ACCESS_TOKEN_ENDPOINT;
    }

    @Override
    public String getAuthorizationUrl(OAuthConfig config) {
        return String.format(AUTHORIZE_URL,
                new Object[]{config.getApiKey(), PropertiesHolder.getProperty("slack.scope"),
                        OAuthEncoder.encode(config.getCallback())});
    }

    public Verb getAccessTokenVerb() {
        return Verb.POST;
    }

    public AccessTokenExtractor getAccessTokenExtractor() {
        return new GoogleJsonTokenExtractor();
    }

    public OAuthService createService(OAuthConfig config) {
        return new GoogleOAuthServiceImpl(this, config);
    }
}
