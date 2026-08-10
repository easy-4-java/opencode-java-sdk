package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * Represents an OpenCode session.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#createSession(String)
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#getSession(String)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Session {

    private String id;
    private String title;

    @JsonProperty("parent_id")
    private String parentId;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    private Map<String, Object> metadata;
}
