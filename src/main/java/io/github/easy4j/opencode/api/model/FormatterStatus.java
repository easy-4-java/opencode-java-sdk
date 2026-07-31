package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Formatter 状态，对应 {@code GET /formatter} 响应元素。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FormatterStatus {

    private String name;

    private Boolean enabled;

    private String command;
}