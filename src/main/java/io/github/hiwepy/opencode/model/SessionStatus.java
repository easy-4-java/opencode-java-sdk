package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Session 状态信息（GET /session/status）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionStatus {

    @JsonProperty("sessionID")
    private String sessionID;

    private String status;
    private String agent;

    @JsonProperty("modelID")
    private String modelID;

    @JsonProperty("providerID")
    private String providerID;

    @JsonProperty("isRunning")
    private boolean running;

    @JsonProperty("messageCount")
    private int messageCount;
}
