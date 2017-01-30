package org.ttnmapper.ttnmapperv2;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.OAuth2AccessTokenJsonExtractor;
import com.github.scribejava.core.extractors.TokenExtractor;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.Verb;

public class TheThingsNetworkOathApi extends DefaultApi20 {

    protected TheThingsNetworkOathApi() {
    }

    private static class InstanceHolder {
        private static final TheThingsNetworkOathApi INSTANCE = new TheThingsNetworkOathApi();
    }

    public static TheThingsNetworkOathApi instance() {
        return InstanceHolder.INSTANCE;
    }

    @Override
    public Verb getAccessTokenVerb() {
        return Verb.POST;
    }

    @Override
    public String getAccessTokenEndpoint() {
        return "https://account.thethingsnetwork.org/users/token";
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return "https://account.thethingsnetwork.org/users/authorize";
    }

    @Override
    public TokenExtractor<OAuth2AccessToken> getAccessTokenExtractor() {
        return OAuth2AccessTokenJsonExtractor.instance();
    }
}
