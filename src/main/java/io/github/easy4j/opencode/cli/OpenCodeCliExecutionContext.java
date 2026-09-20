package io.github.easy4j.opencode.cli;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Overrides that apply to one CLI child-process execution only.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */
@Data
public class OpenCodeCliExecutionContext {

    /** Whether the child starts from the parent JVM environment. */
    private boolean inheritParentEnvironment = true;

    /** Per-execution environment overrides. */
    private final Map<String, String> environment = new LinkedHashMap<>();

    /** Optional per-execution working directory override. */
    private String workingDirectory;

    public OpenCodeCliExecutionContext environment(String name, String value) {
        if (value == null) {
            environment.remove(name);
        } else {
            environment.put(name, value);
        }
        return this;
    }

    public OpenCodeCliExecutionContext inheritParentEnvironment(boolean value) {
        this.inheritParentEnvironment = value;
        return this;
    }

    public OpenCodeCliExecutionContext workingDirectory(String value) {
        this.workingDirectory = value;
        return this;
    }
}
