package io.github.easy4j.opencode.api.model;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenCodeConfigContractTest {

    private final ObjectMapper mapper = new JsonMapper();

    @Test
    void unknownRootFieldsMustSurviveRoundTrip() throws Exception {
        OpenCodeConfig config = mapper.readValue(
                "{\"theme\":\"dark\",\"future_option\":{\"enabled\":true}}",
                OpenCodeConfig.class);

        assertNotNull(config.getExtra(), "unknown root properties must be retained");
        Object future = config.getExtra().get("future_option");
        assertNotNull(future);

        String json = mapper.writeValueAsString(config);
        Map<?, ?> roundTrip = mapper.readValue(json, Map.class);

        assertNotNull(roundTrip.get("future_option"));
    }

    @Test
    void serverConfigurationMustHaveTypedAccessor() {
        Method getter = assertDoesNotThrow(
                () -> OpenCodeConfig.class.getMethod("getServer"),
                "OpenCodeConfig must expose the upstream server configuration");
        assertEquals("getServer", getter.getName());
    }
}
