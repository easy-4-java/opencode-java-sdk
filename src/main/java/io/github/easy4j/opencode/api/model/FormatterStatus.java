package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Represents the status of a code formatter, returned by {@code GET /formatter}.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#listFormatters()
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FormatterStatus {

    private String name;

    private Boolean enabled;

    private String command;
}