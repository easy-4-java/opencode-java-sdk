package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * 待回答的 Question，对应 {@code GET /question}、{@code /session/:id/question} 响应元素。
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