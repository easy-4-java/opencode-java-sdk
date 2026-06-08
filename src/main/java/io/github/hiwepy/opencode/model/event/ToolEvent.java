package io.github.hiwepy.opencode.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * {@code session.next.tool.called} / {@code session.next.tool.success} / {@code session.next.tool.failed}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ToolEvent extends TypedEvent {

    @JsonProperty("toolName")
    public String getToolName() {
        return prop("toolName");
    }

    @JsonProperty("toolCallID")
    public String getToolCallID() {
        return prop("toolCallID");
    }

    /** tool.called 时：传递给工具的输入参数 */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getInput() {
        if (properties == null) return null;
        Object v = properties.get("input");
        return v instanceof Map ? (Map<String, Object>) v : null;
    }

    /** tool.success 时：工具的输出结果 */
    public String getOutput() {
        return prop("output");
    }

    /** tool.failed 时：错误信息 */
    public String getError() {
        return prop("error");
    }
}
