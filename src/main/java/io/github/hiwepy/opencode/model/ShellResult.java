package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Shell 命令执行结果。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShellResult {

    @JsonProperty("exitCode")
    private int exitCode;

    private String stdout;
    private String stderr;

    @JsonProperty("durationMs")
    private long durationMs;
}
