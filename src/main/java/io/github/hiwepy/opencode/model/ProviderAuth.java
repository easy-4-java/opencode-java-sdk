package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Provider 认证方式信息（GET /provider/auth）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProviderAuth {

    @JsonProperty("providerID")
    private String providerID;

    private List<String> methods;

    @JsonProperty("oauthURL")
    private String oauthURL;

    private Map<String, Object> fields;
}
