package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * opencode server 消息发送请求体（POST /session/:id/message）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
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

    /** 消息 ID（用于覆盖/重试时指定） */
    @JsonProperty("messageID")
    private String messageID;

    /** 输出格式 */
    private String format;

    /** 模型变体 */
    private String variant;

    /** 工具开关映射（工具名 → 是否启用） */
    private Map<String, Boolean> tools;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
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
        req.setParts(List.of(p));
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
