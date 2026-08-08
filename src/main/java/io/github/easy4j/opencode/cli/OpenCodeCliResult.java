package io.github.easy4j.opencode.cli;

import lombok.Data;

/**
 * Represents the result of a local {@code opencode} CLI command execution.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see OpenCodeCliExecutor
 */
@Data
public class OpenCodeCliResult {

    private final int exitCode;
    private final String stdout;
    private final String stderr;

    public boolean isSuccess() {
        return exitCode == 0;
    }
}
