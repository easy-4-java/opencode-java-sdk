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
    void autoApprovalMustBeOptInBooleanSwitch() {
        OpenCodeRunOptions disabled = new OpenCodeRunOptions("hello");
        List<String> disabledArgs = Arrays.asList(disabled.toArgs());
        assertFalse(disabledArgs.contains("--auto"), "--auto must be disabled by default");

        OpenCodeRunOptions enabled = new OpenCodeRunOptions("hello");
        Method auto = assertDoesNotThrow(
                () -> OpenCodeRunOptions.class.getMethod("auto", boolean.class),
                "OpenCode run --auto must be explicitly modeled");
        assertDoesNotThrow(() -> auto.invoke(enabled, true));

        List<String> enabledArgs = Arrays.asList(enabled.toArgs());
        assertTrue(enabledArgs.contains("--auto"));
    }

    @Test
    void interactiveModeMustBeExplicitlyModeled() {
        OpenCodeRunOptions options = new OpenCodeRunOptions("hello");
        Method interactive = assertDoesNotThrow(
                () -> OpenCodeRunOptions.class.getMethod("interactive", boolean.class),
                "OpenCode run --interactive must be explicitly modeled");
        assertDoesNotThrow(() -> interactive.invoke(options, true));

        assertTrue(Arrays.asList(options.toArgs()).contains("--interactive"));
    }

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
