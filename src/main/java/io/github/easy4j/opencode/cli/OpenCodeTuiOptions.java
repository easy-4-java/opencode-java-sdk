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
package io.github.easy4j.opencode.cli;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent options for launching the interactive TUI — {@code opencode [project]}.
 *
 * <p>Mirrors the documented flags: {@code --continue/-c}, {@code --session/-s},
 * {@code --fork}, {@code --prompt}, {@code --model/-m}, {@code --agent},
 * {@code --port} and {@code --hostname}. The optional project positional
 * argument is emitted first when set.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see OpenCodeCli#tui(OpenCodeTuiOptions)
 */
public class OpenCodeTuiOptions {

    private String project;
    private boolean continueLast;
    private String sessionId;
    private boolean fork;
    private String prompt;
    private String model;
    private String agent;
    private Integer port;
    private String hostname;

    /** Sets the optional project directory positional argument. */
    public OpenCodeTuiOptions project(String v) { this.project = v; return this; }

    /** Sets the {@code --continue/-c} flag — continue the most recent session. */
    public OpenCodeTuiOptions continueLast(boolean v) { this.continueLast = v; return this; }

    /** Sets the {@code --session/-s} flag — open a specific session id. */
    public OpenCodeTuiOptions sessionId(String v) { this.sessionId = v; return this; }

    /** Sets the {@code --fork} flag — fork when continuing (pairs with --continue/--session). */
    public OpenCodeTuiOptions fork(boolean v) { this.fork = v; return this; }

    /** Sets the {@code --prompt} flag — prompt to bootstrap the session with. */
    public OpenCodeTuiOptions prompt(String v) { this.prompt = v; return this; }

    /** Sets the {@code --model/-m} flag — provider/model identifier. */
    public OpenCodeTuiOptions model(String v) { this.model = v; return this; }

    /** Sets the {@code --agent} flag — agent that serves the session. */
    public OpenCodeTuiOptions agent(String v) { this.agent = v; return this; }

    /** Sets the {@code --port} flag — port of the server to connect to. */
    public OpenCodeTuiOptions port(Integer v) { this.port = v; return this; }

    /** Sets the {@code --hostname} flag — hostname of the server to connect to. */
    public OpenCodeTuiOptions hostname(String v) { this.hostname = v; return this; }

    /**
     * Materialises the options into the CLI argument list — no sub-command
     * word, the TUI is the bare {@code opencode} invocation.
     *
     * @return the CLI argument list.
     */
    public String[] toArgs() {
        List<String> args = new ArrayList<String>();
        if (project != null) { args.add(project); }
        if (continueLast) { args.add("--continue"); }
        if (sessionId != null) { args.add("--session"); args.add(sessionId); }
        if (fork) { args.add("--fork"); }
        if (prompt != null) { args.add("--prompt"); args.add(prompt); }
        if (model != null) { args.add("--model"); args.add(model); }
        if (agent != null) { args.add("--agent"); args.add(agent); }
        if (port != null) { args.add("--port"); args.add(String.valueOf(port)); }
        if (hostname != null) { args.add("--hostname"); args.add(hostname); }
        return args.toArray(new String[0]);
    }
}
