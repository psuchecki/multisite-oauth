package com.oauth.provider;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.Api;
import com.github.scribejava.core.oauth.OAuthService;
import com.oauth.PropertiesHolder;

public class OAuthServiceProvider {
    public static OAuthService getInstance(String appName, Class<? extends Api> providerClass){
        String consumerKey = PropertiesHolder.getProperty(String.format("%s.consumerkey", appName));
        String consumerSecret = PropertiesHolder.getProperty(String.format("%s.consumersecret", appName));
        String callbackUrl = String.format(PropertiesHolder.getProperty("appurl") + "/%s-callback", appName);

        return new ServiceBuilder()
                .provider(providerClass)
                .apiKey(consumerKey)
                .apiSecret(consumerSecret)
                .callback(callbackUrl)
                .build();
    }
}
