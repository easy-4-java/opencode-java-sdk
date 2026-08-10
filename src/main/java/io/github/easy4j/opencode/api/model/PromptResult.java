package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Response body from the OpenCode Server prompt endpoint ({@code POST /session/:id/message}).
 * <p>Structure: {@code { info: Message, parts: Part[] }}. Use {@link #getTextContent()} to
 * extract the concatenated text from all {@code type=text} parts.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see PromptRequest
 * @see Part
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PromptResult {

    /**
     * 本次生成结果对应的消息元数据。
     */
    private Message info;
    /**
     * OpenCode 协议字段 {@code parts} 的集合值；为空表示服务端未返回对应条目。
     */
    private List<Part> parts;

    /**
     * 提取纯文本响应（拼接所有 type=text 的 part）。
     *
     * @return 服务端或 CLI 返回的文本值；无内容时可能为空字符串
     */
    public String getTextContent() {
        if (parts == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Part part : parts) {
            if ("text".equals(part.getType()) && part.getText() != null) {
                sb.append(part.getText());
            }
        }
        return sb.toString();
    }
}
