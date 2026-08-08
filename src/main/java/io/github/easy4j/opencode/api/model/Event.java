package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

/**
 * Represents an SSE event received from the OpenCode Server event stream.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeSseClient
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {

    private String type;
    private Map<String, Object> properties;
}
