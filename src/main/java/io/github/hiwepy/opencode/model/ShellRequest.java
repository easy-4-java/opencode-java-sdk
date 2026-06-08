package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Shell 命令执行请求（POST /session/:id/shell）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShellRequest {

    private String command;

    @JsonProperty("workingDirectory")
    private String workingDirectory;

    private int timeout;

    @JsonProperty("env")
    private java.util.Map<String, String> env;
}
