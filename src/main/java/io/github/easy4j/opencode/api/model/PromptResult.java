package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * opencode server prompt 响应（POST /session/:id/message 的返回体）。
 * <p>
 * 结构为 {@code { info: Message, parts: Part[] }}。
 * </p>
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
