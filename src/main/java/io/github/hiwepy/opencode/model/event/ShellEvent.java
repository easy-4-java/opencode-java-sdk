package io.github.hiwepy.opencode.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * {@code session.next.shell.started} / {@code session.next.shell.ended}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShellEvent extends TypedEvent {

    public String getCommand() {
        return prop("command");
    }

    public int getExitCode() {
        Object v = properties != null ? properties.get("exitCode") : null;
        return v instanceof Number ? ((Number) v).intValue() : -1;
    }

    public String getOutput() {
        return prop("output");
    }
}
