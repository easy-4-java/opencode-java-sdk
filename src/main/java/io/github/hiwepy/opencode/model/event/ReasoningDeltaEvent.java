package io.github.hiwepy.opencode.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * {@code session.next.reasoning.delta} — 推理过程文本增量。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReasoningDeltaEvent extends TypedEvent {

    public String getDelta() {
        return prop("delta");
    }
}
