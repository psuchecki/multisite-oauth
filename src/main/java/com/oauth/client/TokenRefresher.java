package com.oauth.client;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.oauth.OAuthService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static com.github.scribejava.core.model.Verb.POST;

public class TokenRefresher {

    public Token refreshAccessToken(Token accessToken, DefaultApi20 apiProvider, OAuthService service) {
        JsonObject rawReponseJson = new JsonParser().parse(accessToken.getRawResponse()).getAsJsonObject();
        String refresh_token = rawReponseJson.get("refresh_token").getAsString();
        OAuthRequest request = new OAuthRequest(POST, apiProvider.getAccessTokenEndpoint() , service);
        request.addBodyParameter("grant_type", "refresh_token");
        request.addBodyParameter("refresh_token", refresh_token);
        request.addBodyParameter("client_id", service.getConfig().getApiKey());
        request.addBodyParameter("client_secret", service.getConfig().getApiSecret());
        Response response = request.send();

        return apiProvider.getAccessTokenExtractor().extract(response.getBody());
    }
}
