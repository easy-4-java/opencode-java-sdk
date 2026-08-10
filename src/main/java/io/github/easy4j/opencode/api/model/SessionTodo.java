package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents a session todo item, returned by {@code GET /session/:id/todo}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#getSessionTodo(String)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionTodo {

    /**
     * OpenCode Server 分配的唯一标识。
     */
    private String id;

    /**
     * 按到达顺序累计的流式文本内容。
     */
    private String content;

    /**
     * "pending" / "in_progress" / "completed" / "cancelled"
     */
    private String status;

    /**
     * OpenCode 协议字段 {@code priority}；Java 类型为 {@code String}。
     */
    @JsonProperty("Priority")
    private String priority;
}
