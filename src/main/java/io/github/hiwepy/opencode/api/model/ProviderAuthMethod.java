package io.github.hiwepy.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Provider 认证方式，对应 {@code GET /provider/auth} 响应中的 {@code {providerID: [Method]}} 元素。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProviderAuthMethod {

    private String label;

    private String type;

    /** 认证表单字段 schema；provider 自身的 schema（OpenAI key / Anthropic OAuth 等） */
    private Map<String, Object> schema;

    /** 预填字段（OAuth issuer / 入口 URL 等） */
    private Map<String, Object> prefill;

    @JsonProperty("promptOptions")
    private List<Map<String, Object>> promptOptions;
}