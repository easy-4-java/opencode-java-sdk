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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single MCP server entry of the {@code mcp} map in {@code opencode.json}
 * (see the MCP servers documentation).
 *
 * <p>Two shapes exist, discriminated by {@code type}:</p>
 * <ul>
 *   <li><b>local</b> (stdio) — {@code command} array plus optional
 *       {@code environment}, {@code enabled} and {@code timeout} (ms, default 5000).</li>
 *   <li><b>remote</b> (HTTP/SSE) — {@code url} plus optional {@code headers},
 *       {@code oauth}, {@code enabled} and {@code timeout}. OAuth is handled
 *       automatically by OpenCode; {@code oauth} accepts {@code false} to
 *       disable detection or a credentials object ({@code clientId},
 *       {@code clientSecret}, {@code scope}) for pre-registration.</li>
 * </ul>
 *
 * <p>Tools exposed by a server are registered under the server name as
 * namespace prefix and can be disabled via the config {@code tools} map with
 * globs (e.g. {@code "my-server_*": false}).</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see OpenCodeConfig#getMcp()
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class McpServerConfig {

    /** Server type: {@code local} (stdio) or {@code remote} (HTTP/SSE); required. */
    private String type;

    /** Launch command and arguments for a {@code local} server; required for local. */
    private List<String> command;

    /** Environment variables passed to a {@code local} server process. */
    private Map<String, String> environment;

    /** Remote endpoint URL for a {@code remote} server; required for remote. */
    private String url;

    /** Extra HTTP headers sent to a {@code remote} server (e.g. Authorization). */
    private Map<String, String> headers;

    /**
     * OAuth behaviour for a {@code remote} server: {@code null} (auto-detect),
     * {@code false} (disabled) or a credentials object
     * ({@code clientId}/{@code clientSecret}/{@code scope}, supports
     * {@code {env:VAR}} references). Typed as {@link Object} because the
     * wire shape is a boolean-or-object union.
     */
    private Object oauth;

    /** Whether the server is enabled; {@code false} disables it without removing config. */
    private Boolean enabled;

    /** Startup timeout in milliseconds (default 5000). */
    @JsonProperty("timeout")
    private Integer timeout;
}
