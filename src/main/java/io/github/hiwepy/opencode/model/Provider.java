package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * AI Provider 信息（GET /provider）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Provider {

    @JsonProperty("providerID")
    private String providerID;

    private String name;
    private String description;

    @JsonProperty("isAuthenticated")
    private boolean authenticated;

    private List<String> models;
    private Map<String, Object> config;
}
