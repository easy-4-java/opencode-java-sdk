package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents a provider OAuth authorization response, returned by
 * {@code POST /provider/:id/oauth/authorize}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#providerOAuthAuthorize(String, String)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProviderAuthAuthorization {

    /**
     * 认证或远程服务访问 URL。
     */
    private String url;

    /**
     * 认证流程使用的 HTTP 或交互方式。
     */
    private String method;

    /**
     * OAuth 授权码；仅在需要调用方提交时存在。
     */
    @JsonProperty("authorization_code")
    private String authorizationCode;

    /**
     * OAuth 防重放状态值。
     */
    @JsonProperty("state")
    private String state;

    /**
     * 完成认证流程所需的用户提示。
     */
    private String instructions;
}
