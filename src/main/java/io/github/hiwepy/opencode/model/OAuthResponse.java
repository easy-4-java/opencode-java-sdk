package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * OAuth 授权响应（POST /provider/:id/oauth/authorize 等）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuthResponse {

    @JsonProperty("authorizationURL")
    private String authorizationURL;

    @JsonProperty("redirectURL")
    private String redirectURL;

    @JsonProperty("state")
    private String state;

    private String status;
}
