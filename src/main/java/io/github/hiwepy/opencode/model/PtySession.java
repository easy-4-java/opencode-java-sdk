package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * PTY（终端）会话（GET/POST /pty）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PtySession {

    @JsonProperty("ptyID")
    private String ptyID;

    @JsonProperty("sessionID")
    private String sessionID;

    private String shell;

    @JsonProperty("workingDirectory")
    private String workingDirectory;

    private String status;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("lastActiveAt")
    private String lastActiveAt;
}
