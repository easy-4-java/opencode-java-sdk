package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Represents a pending question request, returned by {@code GET /question}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#listQuestions()
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#replyQuestion(String, java.util.List)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionRequest {

    /**
     * OpenCode Server 分配的唯一标识。
     */
    private String id;

    /**
     * 提问所属 session ID
     */
    private String sessionID;

    /**
     * OpenCode 协议字段 {@code options} 的集合值；为空表示服务端未返回对应条目。
     */
    private List<QuestionOption> options;

    /**
     * 提问头部（标题）
     */
    private String header;

    /**
     * 完整问题文本
     */
    private String question;

    /**
     * 选项。
     *
     * @author <a href="https://github.com/loong10k">Loong Wan</a>
     * @since 1.0.0
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuestionOption {
        /**
         * OpenCode 协议字段 {@code label}；Java 类型为 {@code String}。
         */
        private String label;
        /**
         * 资源用途或能力的可读说明。
         */
        private String description;
        /**
         * OpenCode 协议字段 {@code preview}；Java 类型为 {@code String}。
         */
        private String preview;
    }
}
