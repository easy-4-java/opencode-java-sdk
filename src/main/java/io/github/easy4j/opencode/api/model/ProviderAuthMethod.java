package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Represents a provider authentication method, returned in {@code GET /provider/auth}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#listProviderAuthMethods()
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProviderAuthMethod {

    /**
     * OpenCode 协议字段 {@code label}；Java 类型为 {@code String}。
     */
    private String label;

    /**
     * OpenCode 协议中的类型判别值。
     */
    private String type;

    /**
     * 认证表单字段 schema；provider 自身的 schema（OpenAI key / Anthropic OAuth 等）
     */
    private Map<String, Object> schema;

    /**
     * 预填字段（OAuth issuer / 入口 URL 等）
     */
    private Map<String, Object> prefill;

    /**
     * OpenCode 协议字段 {@code promptOptions} 的集合值；为空表示服务端未返回对应条目。
     */
    @JsonProperty("promptOptions")
    private List<Map<String, Object>> promptOptions;
}
