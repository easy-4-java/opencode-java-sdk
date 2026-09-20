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
import java.util.Arrays;
import java.util.List;

/**
 * Fluent options for {@code opencode run} — the non-interactive execution mode.
 *
 * <p>Mirrors every documented flag of the command: {@code --command},
 * {@code --continue/-c}, {@code --session/-s}, {@code --fork}, {@code --share},
 * {@code --model/-m}, {@code --agent}, {@code --file/-f}, {@code --format},
 * {@code --title}, {@code --attach}, {@code --username/-u}, {@code --password/-p},
 * {@code --dir}, {@code --variant}, {@code --thinking} and {@code --port}.
 * The message is always emitted last as the positional argument.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see OpenCodeCli#run(OpenCodeRunOptions)
 */
public class OpenCodeRunOptions {

    private final String message;
    private String command;
    private boolean continueLast;
    private String sessionId;
    private boolean fork;
    private boolean share;
    private String model;
    private String agent;
    private List<String> files;
    private String format;
    private String title;
    private String attach;
    private String username;
    private String password;
    private String dir;
    private String variant;
    private boolean thinking;
    private Integer port;

    /**
     * Creates options bound to the given prompt.
     *
     * @param message the prompt text; must not be {@code null}.
     */
    public OpenCodeRunOptions(String message) {
        this.message = message;
    }

    /** Sets the {@code --command} flag — run a custom command instead of a prompt. */
    public OpenCodeRunOptions command(String v) { this.command = v; return this; }

    /** Sets the {@code --continue/-c} flag — continue the most recent session. */
    public OpenCodeRunOptions continueLast(boolean v) { this.continueLast = v; return this; }

    /** Sets the {@code --session/-s} flag — continue a specific session id. */
    public OpenCodeRunOptions sessionId(String v) { this.sessionId = v; return this; }

    /** Sets the {@code --fork} flag — fork the session instead of mutating it. */
    public OpenCodeRunOptions fork(boolean v) { this.fork = v; return this; }

    /** Sets the {@code --share} flag — share the session after the run. */
    public OpenCodeRunOptions share(boolean v) { this.share = v; return this; }

    /**
     * @deprecated OpenCode models {@code --share} as a boolean switch. Any non-null legacy
     * value enables the switch and the value itself is ignored.
     */
    @Deprecated
    public OpenCodeRunOptions share(String v) { this.share = v != null; return this; }

    /** Sets the {@code --model/-m} flag — provider/model identifier. */
    public OpenCodeRunOptions model(String v) { this.model = v; return this; }

    /** Sets the {@code --agent} flag — agent that executes the request. */
    public OpenCodeRunOptions agent(String v) { this.agent = v; return this; }

    /** Sets one or more {@code --file/-f} flags — files to attach to the prompt. */
    public OpenCodeRunOptions files(List<String> v) { this.files = v; return this; }

    /** Sets {@code --file/-f} flags from varargs. */
    public OpenCodeRunOptions files(String... v) { this.files = Arrays.asList(v); return this; }

    /** Sets the {@code --format} flag — {@code default} (formatted) or {@code json} (raw events). */
    public OpenCodeRunOptions format(String v) { this.format = v; return this; }

    /** Sets the {@code --title} flag — session title for the run. */
    public OpenCodeRunOptions title(String v) { this.title = v; return this; }

    /** Sets the {@code --attach} flag — attach to a running server (e.g. {@code http://localhost:4096}). */
    public OpenCodeRunOptions attach(String v) { this.attach = v; return this; }

    /** Sets the {@code --username/-u} flag — basic-auth username of the attached server. */
    public OpenCodeRunOptions username(String v) { this.username = v; return this; }

    /** Sets the {@code --password/-p} flag — basic-auth password of the attached server. */
    public OpenCodeRunOptions password(String v) { this.password = v; return this; }

    /** Sets the {@code --dir} flag — working directory for the run. */
    public OpenCodeRunOptions dir(String v) { this.dir = v; return this; }

    /** Sets the {@code --variant} flag — provider-specific reasoning effort (e.g. {@code high}). */
    public OpenCodeRunOptions variant(String v) { this.variant = v; return this; }

    /** Sets the {@code --thinking} flag — show thinking blocks in output. */
    public OpenCodeRunOptions thinking(boolean v) { this.thinking = v; return this; }

    /** Sets the {@code --port} flag — port of the attached server. */
    public OpenCodeRunOptions port(Integer v) { this.port = v; return this; }

    /**
     * Materialises the options into the CLI argument list, beginning with
     * {@code "run"} and ending with the message.
     *
     * @return the CLI argument list.
     */
    public String[] toArgs() {
        List<String> args = new ArrayList<String>();
        args.add("run");
        if (command != null) { args.add("--command"); args.add(command); }
        if (continueLast) { args.add("--continue"); }
        if (sessionId != null) { args.add("--session"); args.add(sessionId); }
        if (fork) { args.add("--fork"); }
        if (share) { args.add("--share"); }
        if (model != null) { args.add("--model"); args.add(model); }
        if (agent != null) { args.add("--agent"); args.add(agent); }
        if (files != null) {
            for (String f : files) { args.add("--file"); args.add(f); }
        }
        if (format != null) { args.add("--format"); args.add(format); }
        if (title != null) { args.add("--title"); args.add(title); }
        if (attach != null) { args.add("--attach"); args.add(attach); }
        if (username != null) { args.add("--username"); args.add(username); }
        if (password != null) { args.add("--password"); args.add(password); }
        if (dir != null) { args.add("--dir"); args.add(dir); }
        if (variant != null) { args.add("--variant"); args.add(variant); }
        if (thinking) { args.add("--thinking"); }
        if (port != null) { args.add("--port"); args.add(String.valueOf(port)); }
        if (message != null) { args.add(message); }
        return args.toArray(new String[0]);
    }

    /** Returns the prompt text bound at construction. */
    public String getMessage() {
        return message;
    }
}
