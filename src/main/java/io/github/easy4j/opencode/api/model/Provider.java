package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Represents an OpenCode provider, returned by {@code GET /provider}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see ProviderList
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#listProviders()
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Provider {

    private String id;

    private String name;

    private String description;

    private String source;

    /** 嵌套 {@link ProviderAuthMethod} 列表 */
    @JsonProperty("authMethods")
    private List<ProviderAuthMethod> authMethods;

    /** 嵌套 Provider 模型对象（modelId -> ModelInfo） */
    private Map<String, Object> models;

    private Map<String, Object> options;
}