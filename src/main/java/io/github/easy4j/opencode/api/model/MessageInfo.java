package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Detailed information for a single message, returned by {@code GET /session/:id/message/:messageID}.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#getMessage(String, String)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageInfo {

    private Message info;

    private List<Part> parts;
}