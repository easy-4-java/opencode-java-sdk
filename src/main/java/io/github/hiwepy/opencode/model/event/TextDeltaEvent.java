package io.github.hiwepy.opencode.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * {@code session.next.text.delta} — 流式文本增量。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TextDeltaEvent extends TypedEvent {

    /** 增量文本片段 */
    public String getDelta() {
        return prop("delta");
    }

    /** 是否为最后一个 delta（text.ended 之前） */
    public boolean isFinal() {
        return propBool("final");
    }
}
