package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * PTY shell 列表（GET /pty/shells）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PtyShellInfo {

    private List<String> shells;

    @JsonProperty("defaultShell")
    private String defaultShell;
}
