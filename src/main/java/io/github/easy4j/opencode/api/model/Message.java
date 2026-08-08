package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents an OpenCode message within a session.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see MessageInfo
 * @see PromptResult
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
