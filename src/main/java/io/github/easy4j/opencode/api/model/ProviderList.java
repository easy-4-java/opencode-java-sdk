package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Container for the OpenCode provider list, returned by {@code GET /provider}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see Provider
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#listProviders()
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