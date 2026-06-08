package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * OAuth 认证请求/响应（PUT/DELETE /auth/:providerID）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthCredential {

    private String key;

    @JsonProperty("apiKey")
    private String apiKey;

    @JsonProperty("apiBase")
    private String apiBase;

    @JsonProperty("providerID")
    private String providerID;
}
