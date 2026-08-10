package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents a provider OAuth authorization response, returned by
 * {@code POST /provider/:id/oauth/authorize}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#providerOAuthAuthorize(String, String)
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