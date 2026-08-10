package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * Represents an OpenCode session.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#createSession(String)
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#getSession(String)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Session {

    /**
     * OpenCode Server 分配的唯一标识。
     */
    private String id;
    /**
     * 会话展示标题，也用于稳定会话查找。
     */
    private String title;

    /**
     * 父会话 ID；顶层会话为空。
     */
    @JsonProperty("parent_id")
    private String parentId;

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

    /**
     * 服务端附带的可扩展元数据映射。
     */
    private Map<String, Object> metadata;
}
