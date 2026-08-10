package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Request body for sending a message to the OpenCode Server ({@code POST /session/:id/message}).
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see PromptResult
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#prompt(String, PromptRequest)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PromptRequest {

    /**
     * 消息部件列表（type=text 时填充 text 字段）。
     */
    private List<Part> parts;

    /**
     * 指定模型，格式 {@code { "providerID": "...", "modelID": "..." }}。
     */
    private ModelRef model;

    /**
     * 指定 agent。
     */
    private String agent;

    /**
     * 仅注入上下文，不触发 AI 响应。
     */
    private Boolean noReply;

    /**
     * 系统 prompt 附加内容。
     */
    private String system;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ModelRef {
        private String providerID;
        private String modelID;
    }

    /**
     * 快捷构造：纯文本 prompt。
     */
    public static PromptRequest ofText(String text) {
        Part p = new Part();
        p.setType("text");
        p.setText(text);
        PromptRequest req = new PromptRequest();
        req.setParts(Collections.singletonList(p));
        return req;
    }

    /**
     * 快捷构造：带模型的纯文本 prompt。
     */
    public static PromptRequest ofText(String text, String providerID, String modelID) {
        PromptRequest req = ofText(text);
        req.setModel(new ModelRef(providerID, modelID));
        return req;
    }
}
