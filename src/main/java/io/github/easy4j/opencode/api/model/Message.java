package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents an OpenCode message within a session.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see MessageInfo
 * @see PromptResult
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {

    /**
     * OpenCode Server 分配的唯一标识。
     */
    private String id;

    /**
     * 事件或消息所属的 OpenCode 会话 ID。
     */
    @JsonProperty("session_id")
    private String sessionId;

    /**
     * 消息角色，例如 user、assistant 或 system。
     */
    private String role;

    /**
     * 创建时间，由 OpenCode Server 返回。
     */
    @JsonProperty("created_at")
    private String createdAt;

    /**
     * 最后更新时间，由 OpenCode Server 返回。
     */
    @JsonProperty("updated_at")
    private String updatedAt;
}
