/*
 * Copyright (c) 2018-present, easy-4-java (https://github.com/easy-4-java).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.easy4j.opencode.api.model;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link McpServerConfig}: JSON round-trips for both the
 * {@code local} (stdio) and {@code remote} (HTTP/SSE) shapes plus tolerance
 * of unknown fields, mirroring the MCP servers documentation.
 *
 * @since 3.0.0
 */
class McpServerConfigTest {

    private final ObjectMapper mapper = new JsonMapper();

    @Test
    void shouldRoundTripLocalServer() throws Exception {
        McpServerConfig config = McpServerConfig.builder()
                .type("local")
                .command(Arrays.asList("npx", "-y", "@modelcontextprotocol/server-everything"))
                .environment(new LinkedHashMap<String, String>())
                .enabled(true)
                .timeout(10_000)
                .build();

        String json = mapper.writeValueAsString(config);
        McpServerConfig decoded = mapper.readValue(json, McpServerConfig.class);

        assertEquals("local", decoded.getType());
        assertEquals(Arrays.asList("npx", "-y", "@modelcontextprotocol/server-everything"),
                decoded.getCommand());
        assertEquals(Boolean.TRUE, decoded.getEnabled());
        assertEquals(Integer.valueOf(10_000), decoded.getTimeout());
        assertNull(decoded.getUrl());
    }

    @Test
    void shouldRoundTripRemoteServer() throws Exception {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer MY_API_KEY");
        McpServerConfig config = McpServerConfig.builder()
                .type("remote")
                .url("https://my-mcp-server.com")
                .headers(headers)
                .enabled(true)
                .build();

        String json = mapper.writeValueAsString(config);
        McpServerConfig decoded = mapper.readValue(json, McpServerConfig.class);

        assertEquals("remote", decoded.getType());
        assertEquals("https://my-mcp-server.com", decoded.getUrl());
        assertEquals("Bearer MY_API_KEY", decoded.getHeaders().get("Authorization"));
        assertNull(decoded.getCommand());
    }

    @Test
    void shouldTolerateUnknownFieldsAndOauthFalse() throws Exception {
        String json = "{\"type\":\"remote\",\"url\":\"https://x.com\",\"oauth\":false,"
                + "\"unknownField\":123}";
        McpServerConfig decoded = mapper.readValue(json, McpServerConfig.class);

        assertEquals("remote", decoded.getType());
        assertEquals(Boolean.FALSE, decoded.getOauth());
        assertTrue(decoded.getEnabled() == null);
    }

    @Test
    void shouldDeserializeOauthCredentialsObject() throws Exception {
        String json = "{\"type\":\"remote\",\"url\":\"https://x.com\","
                + "\"oauth\":{\"clientId\":\"cid\",\"clientSecret\":\"{env:SECRET}\",\"scope\":\"read\"}}";
        McpServerConfig decoded = mapper.readValue(json, McpServerConfig.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> oauth = (Map<String, Object>) decoded.getOauth();
        assertEquals("cid", oauth.get("clientId"));
        assertEquals("{env:SECRET}", oauth.get("clientSecret"));
    }
}
