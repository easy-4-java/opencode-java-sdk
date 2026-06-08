package io.github.hiwepy.opencode.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * {@code session.next.step.started} / {@code session.next.step.ended} / {@code session.next.step.failed}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StepEvent extends TypedEvent {

    public String getAgent() {
        return prop("agent");
    }

    public String getModel() {
        return prop("model");
    }

    /** step.ended 时：finish reason */
    @JsonProperty("finishReason")
    public String getFinishReason() {
        return prop("finishReason");
    }

    /** step.ended 时：token 用量 */
    @JsonProperty("inputTokens")
    public long getInputTokens() {
        Object v = properties != null ? properties.get("inputTokens") : null;
        return v instanceof Number ? ((Number) v).longValue() : 0;
    }

    @JsonProperty("outputTokens")
    public long getOutputTokens() {
        Object v = properties != null ? properties.get("outputTokens") : null;
        return v instanceof Number ? ((Number) v).longValue() : 0;
    }
}
