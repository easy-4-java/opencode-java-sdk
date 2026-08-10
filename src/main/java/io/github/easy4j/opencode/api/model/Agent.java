package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents an OpenCode agent, returned by {@code GET /agent}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#listAgents()
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Agent {

    private String name;
    private String description;
    private String mode;

    @JsonProperty("model")
    private Object model;
}
