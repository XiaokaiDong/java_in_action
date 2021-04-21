package io.tt.oauth.domain;

import lombok.Data;

@Data
public class OAuth2Client {
    private String id;
    private String secret;
    private String redirectUri;
    private String scope;
    private String oauthUrl;
    private String accessTokenEndPoint;
    private String userInfoEndPoint;
}
