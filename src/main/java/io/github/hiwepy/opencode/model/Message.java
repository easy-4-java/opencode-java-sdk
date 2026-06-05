package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * OpenCode Message。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {

    private String id;

    @JsonProperty("session_id")
    private String sessionId;

    private String role;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;
}
