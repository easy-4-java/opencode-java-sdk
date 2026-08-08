package io.github.easy4j.opencode.cli.availability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OpenCodeCliAvailabilityStatus}.
 */
class OpenCodeCliAvailabilityStatusTest {

    @Test
    void shouldContainExpectedValues() {
        OpenCodeCliAvailabilityStatus[] values = OpenCodeCliAvailabilityStatus.values();
        assertEquals(8, values.length);
    }

    @Test
    void shouldHaveAvailableStatus() {
        assertNotNull(OpenCodeCliAvailabilityStatus.AVAILABLE);
    }

    @Test
    void shouldHaveExecutableNotConfiguredStatus() {
        assertNotNull(OpenCodeCliAvailabilityStatus.EXECUTABLE_NOT_CONFIGURED);
    }

    @Test
    void shouldHaveExecutableNotFoundStatus() {
        assertNotNull(OpenCodeCliAvailabilityStatus.EXECUTABLE_NOT_FOUND);
    }

    @Test
    void shouldHaveExecutableNotExecutableStatus() {
        assertNotNull(OpenCodeCliAvailabilityStatus.EXECUTABLE_NOT_EXECUTABLE);
    }

    @Test
    void shouldHaveSpawnFailedStatus() {
        assertNotNull(OpenCodeCliAvailabilityStatus.SPAWN_FAILED);
    }

    @Test
    void shouldHaveNonZeroExitStatus() {
        assertNotNull(OpenCodeCliAvailabilityStatus.NON_ZERO_EXIT);
    }

    @Test
    void shouldHaveTimeoutStatus() {
        assertNotNull(OpenCodeCliAvailabilityStatus.TIMEOUT);
    }

    @Test
    void shouldHaveFailedStatus() {
        assertNotNull(OpenCodeCliAvailabilityStatus.FAILED);
    }

    @Test
    void shouldResolveFromValue() {
        assertEquals(OpenCodeCliAvailabilityStatus.AVAILABLE,
                OpenCodeCliAvailabilityStatus.valueOf("AVAILABLE"));
    }
}
