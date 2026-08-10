package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Represents the status of a code formatter, returned by {@code GET /formatter}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#listFormatters()
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FormatterStatus {

    /**
     * 资源的可读名称。
     */
    private String name;

    /**
     * 是否启用当前子系统。
     */
    private Boolean enabled;

    /**
     * OpenCode 协议字段 {@code command}；Java 类型为 {@code String}。
     */
    private String command;
}
