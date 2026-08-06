package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * OpenCode Agent 信息。
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
