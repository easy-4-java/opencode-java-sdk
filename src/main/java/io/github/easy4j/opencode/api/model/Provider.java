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
 * @since 1.0.0
 * @see ProviderList
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#listProviders()
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Provider {

    /**
     * OpenCode Server 分配的唯一标识。
     */
    private String id;

    /**
     * 资源的可读名称。
     */
    private String name;

    /**
     * 资源用途或能力的可读说明。
     */
    private String description;

    /**
     * OpenCode 协议字段 {@code source}；Java 类型为 {@code String}。
     */
    private String source;

    /**
     * 嵌套 {@link ProviderAuthMethod} 列表
     */
    @JsonProperty("authMethods")
    private List<ProviderAuthMethod> authMethods;

    /**
     * 嵌套 Provider 模型对象（modelId -> ModelInfo）
     */
    private Map<String, Object> models;

    /**
     * OpenCode 协议字段 {@code options} 的集合值；为空表示服务端未返回对应条目。
     */
    private Map<String, Object> options;
}
