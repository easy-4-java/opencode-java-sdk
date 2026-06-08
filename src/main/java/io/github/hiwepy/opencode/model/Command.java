package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * 可用命令（GET /command）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Command {

    private String name;
    private String description;

    private Map<String, Object> args;

    @JsonProperty("agentOnly")
    private boolean agentOnly;
}
