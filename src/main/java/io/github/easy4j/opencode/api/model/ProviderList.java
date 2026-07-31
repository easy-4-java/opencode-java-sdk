package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * OpenCode Provider 列表容器，对应 {@code GET /provider} 响应整体。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProviderList {

    private List<Provider> all;

    /** 默认 provider/model 映射 */
    private Map<String, String> defaults;

    /** 默认映射（同 defaults 别名） */
    private Map<String, String> default_;

    private List<String> connected;
}