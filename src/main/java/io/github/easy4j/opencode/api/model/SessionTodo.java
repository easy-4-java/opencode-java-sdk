package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Session todo 元素，对应 {@code GET /session/:id/todo} 响应元素。
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