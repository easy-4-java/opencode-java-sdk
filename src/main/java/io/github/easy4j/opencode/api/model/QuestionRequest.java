package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Represents a pending question request, returned by {@code GET /question}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#listQuestions()
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#replyQuestion(String, java.util.List)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionRequest {

    private String id;

    /** 提问所属 session ID */
    private String sessionID;

    private List<QuestionOption> options;

    /** 提问头部（标题） */
    private String header;

    /** 完整问题文本 */
    private String question;

    /**
     * 选项。
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuestionOption {
        private String label;
        private String description;
        private String preview;
    }
}