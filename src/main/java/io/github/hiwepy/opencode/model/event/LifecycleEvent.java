package io.github.hiwepy.opencode.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * {@code session.next.prompted} / {@code session.next.compaction.started} 等生命周期事件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LifecycleEvent extends TypedEvent {

    public String getAgent() {
        return prop("agent");
    }

    public String getModel() {
        return prop("model");
    }

    public String getReason() {
        return prop("reason");
    }
}
