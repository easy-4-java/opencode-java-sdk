package io.github.hiwepy.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Provider OAuth 授权响应，对应 {@code POST /provider/:id/oauth/authorize}。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProviderAuthAuthorization {

    private String url;

    private String method;

    @JsonProperty("authorization_code")
    private String authorizationCode;

    @JsonProperty("state")
    private String state;

    private String instructions;
}