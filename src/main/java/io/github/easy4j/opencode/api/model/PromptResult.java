package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Response body from the OpenCode Server prompt endpoint ({@code POST /session/:id/message}).
 *
 * <p>Structure: {@code { info: Message, parts: Part[] }}. Use {@link #getTextContent()} to
 * extract the concatenated text from all {@code type=text} parts.</p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see PromptRequest
 * @see Part
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PromptResult {

    private Message info;
    private List<Part> parts;

    /**
     * 提取纯文本响应（拼接所有 type=text 的 part）。
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
