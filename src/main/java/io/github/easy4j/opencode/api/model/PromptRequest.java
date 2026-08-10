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
 * @since 1.0.0
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

    /**
     * 模型提供方与模型 ID 的协议引用。
     *
     * @author <a href="https://github.com/loong10k">Loong Wan</a>
     * @since 1.0.0
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ModelRef {
        /**
         * OpenCode 协议字段 {@code providerID}；Java 类型为 {@code String}。
         */
        private String providerID;
        /**
         * OpenCode 协议字段 {@code modelID}；Java 类型为 {@code String}。
         */
        private String modelID;
    }

    /**
     * 快捷构造：纯文本 prompt。
     *
     * @param text 发送给模型的文本内容
     * @return OpenCode SDK 返回的Prompt 请求对象
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
     *
     * @param text 发送给模型的文本内容
     * @param providerID 模型提供方 ID
     * @param modelID 模型 ID
     * @return OpenCode SDK 返回的Prompt 请求对象
     */
    public static PromptRequest ofText(String text, String providerID, String modelID) {
        PromptRequest req = ofText(text);
        req.setModel(new ModelRef(providerID, modelID));
        return req;
    }
}
