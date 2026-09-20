package io.github.easy4j.opencode.cli;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream CLI option contract tests for {@link OpenCodeRunOptions}.
 */
class OpenCodeRunOptionsContractTest {

    @Test
    void shareMustBeBooleanSwitchWithoutValue() {
        OpenCodeRunOptions options = new OpenCodeRunOptions("hello");

        Method share = assertDoesNotThrow(
                () -> OpenCodeRunOptions.class.getMethod("share", boolean.class),
                "OpenCode run --share is a boolean switch upstream");

        assertDoesNotThrow(() -> share.invoke(options, true));

        List<String> args = Arrays.asList(options.toArgs());
        int shareIndex = args.indexOf("--share");

        assertTrue(shareIndex >= 0, "--share must be emitted when enabled");
        assertFalse(shareIndex + 1 < args.size() && "true".equals(args.get(shareIndex + 1)),
                "--share must not consume a value");
    }
}
