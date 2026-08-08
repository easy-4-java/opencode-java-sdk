package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents a session todo item, returned by {@code GET /session/:id/todo}.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#getSessionTodo(String)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionTodo {

    private String id;

    private String content;

    /** "pending" / "in_progress" / "completed" / "cancelled" */
    private String status;

    @JsonProperty("Priority")
    private String priority;
}