package io.github.easy4j.opencode.api.model;

import io.github.easy4j.opencode.api.sse.SseEvent;

import io.github.easy4j.opencode.api.sse.StreamingChatResponse;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for all model POJOs in {@code io.github.easy4j.opencode.api.model}.
 */
class ModelClassesTest {

    // ============================================================
    // Agent
    // ============================================================

    @Test
    void shouldCreateAgentWithGettersAndSetters() {
        Agent agent = new Agent();
        agent.setName("coder");
        agent.setDescription("Coding agent");
        agent.setMode("subagent");
        agent.setModel("anthropic/claude-sonnet-4-5");
        assertEquals("coder", agent.getName());
        assertEquals("Coding agent", agent.getDescription());
        assertEquals("subagent", agent.getMode());
        assertNotNull(agent.getModel());
    }

    // ============================================================
    // ChatMessage
    // ============================================================

    @Test
    void shouldCreateChatMessageWithRoleAndContent() {
        ChatMessage msg = new ChatMessage("user", "hello");
        assertEquals("user", msg.getRole());
        assertEquals("hello", msg.getContent());
    }

    @Test
    void shouldCreateUserMessageViaFactory() {
        ChatMessage msg = ChatMessage.user("hi");
        assertEquals("user", msg.getRole());
        assertEquals("hi", msg.getContent());
    }

    @Test
    void shouldCreateSystemMessageViaFactory() {
        ChatMessage msg = ChatMessage.system("behave");
        assertEquals("system", msg.getRole());
        assertEquals("behave", msg.getContent());
    }

    @Test
    void shouldCreateAssistantMessageViaFactory() {
        ChatMessage msg = ChatMessage.assistant("reply");
        assertEquals("assistant", msg.getRole());
        assertEquals("reply", msg.getContent());
    }

    @Test
    void shouldSupportNoArgsConstructor() {
        ChatMessage msg = new ChatMessage();
        assertNull(msg.getRole());
        assertNull(msg.getContent());
    }

    // ============================================================
    // ChatRequest
    // ============================================================

    @Test
    void shouldCreateChatRequestWithAllFields() {
        ChatRequest req = new ChatRequest();
        req.setModel("anthropic/claude-sonnet-4-5");
        req.setMessages(Collections.singletonList(ChatMessage.user("hi")));
        req.setStream(true);
        req.setStreamOptions(Map.of("include_usage", true));
        req.setAgent("coder");
        req.setSystem("system prompt");
        req.setMaxTokens(1024);
        req.setTemperature(0.7);
        req.setTopP(0.9);
        req.setUser("user-1");

        assertEquals("anthropic/claude-sonnet-4-5", req.getModel());
        assertEquals(1, req.getMessages().size());
        assertTrue(req.getStream());
        assertEquals("coder", req.getAgent());
        assertEquals("system prompt", req.getSystem());
        assertEquals(1024, req.getMaxTokens());
        assertEquals(0.7, req.getTemperature());
        assertEquals(0.9, req.getTopP());
        assertEquals("user-1", req.getUser());
    }

    @Test
    void shouldCreateChatRequestOfUser() {
        ChatRequest req = ChatRequest.ofUser("hello");
        assertEquals(1, req.getMessages().size());
        assertEquals("user", req.getMessages().get(0).getRole());
        assertNull(req.getModel());
    }

    @Test
    void shouldCreateChatRequestOfUserWithModel() {
        ChatRequest req = ChatRequest.ofUser("hello", "anthropic/claude-sonnet-4-5");
        assertEquals("anthropic/claude-sonnet-4-5", req.getModel());
    }

    // ============================================================
    // ChatResponse
    // ============================================================

    @Test
    void shouldGetContentFromChatResponse() {
        ChatResponse resp = new ChatResponse();
        ChatResponse.Choice choice = new ChatResponse.Choice();
        choice.setIndex(0);
        choice.setMessage(new ChatMessage("assistant", "hello world"));
        choice.setFinishReason("stop");
        resp.setChoices(Collections.singletonList(choice));

        assertEquals("hello world", resp.getContent());
    }

    @Test
    void shouldReturnNullContentWhenNoChoices() {
        ChatResponse resp = new ChatResponse();
        assertNull(resp.getContent());
    }

    @Test
    void shouldReturnNullContentWhenEmptyChoices() {
        ChatResponse resp = new ChatResponse();
        resp.setChoices(Collections.emptyList());
        assertNull(resp.getContent());
    }

    @Test
    void shouldReturnNullContentWhenMessageIsNull() {
        ChatResponse resp = new ChatResponse();
        ChatResponse.Choice choice = new ChatResponse.Choice();
        choice.setMessage(null);
        resp.setChoices(Collections.singletonList(choice));
        assertNull(resp.getContent());
    }

    @Test
    void shouldSetAndGetUsage() {
        ChatResponse.Usage usage = new ChatResponse.Usage();
        usage.setPromptTokens(10);
        usage.setCompletionTokens(20);
        usage.setTotalTokens(30);
        assertEquals(10, usage.getPromptTokens());
        assertEquals(20, usage.getCompletionTokens());
        assertEquals(30, usage.getTotalTokens());
    }

    // ============================================================
    // StreamingChatResponse
    // ============================================================

    @Test
    void shouldAccumulateDeltasAndFinish() {
        StreamingChatResponse stream = new StreamingChatResponse();
        StringBuilder received = new StringBuilder();
        stream.onDelta(received::append);

        stream.acceptDelta("Hello");
        stream.acceptDelta(" World");

        assertEquals("Hello World", stream.getAccumulatedContent());
        assertEquals("Hello World", received.toString());

        stream.finish();
        assertTrue(stream.isDone());
        assertFalse(stream.isCompletedExceptionally());
        assertEquals("Hello World", stream.join());
    }

    @Test
    void shouldFailStreamOnException() {
        StreamingChatResponse stream = new StreamingChatResponse();
        RuntimeException error = new RuntimeException("boom");
        stream.fail(error);

        assertTrue(stream.isDone());
        assertTrue(stream.isCompletedExceptionally());
        assertThrows(RuntimeException.class, stream::join);
    }

    @Test
    void shouldHandleNullAndEmptyDelta() {
        StreamingChatResponse stream = new StreamingChatResponse();
        stream.acceptDelta(null);
        stream.acceptDelta("");
        assertEquals("", stream.getAccumulatedContent());
    }

    @Test
    void shouldWorkWithoutDeltaConsumer() {
        StreamingChatResponse stream = new StreamingChatResponse();
        stream.acceptDelta("data");
        assertEquals("data", stream.getAccumulatedContent());
    }

    // ============================================================
    // Command
    // ============================================================

    @Test
    void shouldCreateCommand() {
        Command cmd = new Command();
        cmd.setName("/help");
        cmd.setDescription("Show help");
        cmd.setTemplate("help {{arg}}");
        cmd.setArgs(List.of("arg"));
        cmd.setAgent("coder");
        assertEquals("/help", cmd.getName());
        assertEquals("Show help", cmd.getDescription());
        assertEquals("help {{arg}}", cmd.getTemplate());
        assertEquals(1, cmd.getArgs().size());
        assertEquals("coder", cmd.getAgent());
    }

    // ============================================================
    // SseEvent
    // ============================================================

    @Test
    void shouldCreateEvent() {
        SseEvent event = new SseEvent();
        event.setType("session.idle");
        event.setProperties(Map.of("sessionID", "sess-1"));
        assertEquals("session.idle", event.getType());
        assertEquals("sess-1", event.getProperties().get("sessionID"));
    }

    // ============================================================
    // FileContent
    // ============================================================

    @Test
    void shouldCreateFileContent() {
        FileContent fc = new FileContent();
        fc.setType("file");
        fc.setContent("hello");
        fc.setEncoding("utf-8");
        fc.setMimeType("text/plain");
        fc.setLastModified(12345L);
        fc.setSize(5L);
        assertEquals("file", fc.getType());
        assertEquals("hello", fc.getContent());
        assertEquals("utf-8", fc.getEncoding());
        assertEquals("text/plain", fc.getMimeType());
        assertEquals(12345L, fc.getLastModified());
        assertEquals(5L, fc.getSize());
    }

    // ============================================================
    // FileDiff
    // ============================================================

    @Test
    void shouldCreateFileDiff() {
        FileDiff diff = new FileDiff();
        diff.setPath("src/Main.java");
        diff.setOldPath("src/Main.java");
        diff.setStatus("modified");
        diff.setAdditions(5);
        diff.setDeletions(2);
        diff.setPatch("@@ -1,3 +1,6 @@");
        assertEquals("src/Main.java", diff.getPath());
        assertEquals("modified", diff.getStatus());
        assertEquals(5, diff.getAdditions());
        assertEquals(2, diff.getDeletions());
    }

    // ============================================================
    // FileNode
    // ============================================================

    @Test
    void shouldCreateFileNode() {
        FileNode node = new FileNode();
        node.setPath("src/Main.java");
        node.setType("file");
        node.setAbsolute("/project/src/Main.java");
        node.setIgnored(false);
        assertEquals("src/Main.java", node.getPath());
        assertEquals("file", node.getType());
        assertFalse(node.getIgnored());
    }

    // ============================================================
    // FileSearchResult
    // ============================================================

    @Test
    void shouldCreateFileSearchResult() {
        FileSearchResult result = new FileSearchResult();
        result.setPath("src/Main.java");
        result.setLines(List.of("public class Main {}"));
        result.setLineNumber(1);
        result.setAbsoluteOffset(0);
        result.setSubmatches(List.of());
        assertEquals("src/Main.java", result.getPath());
        assertEquals(1, result.getLineNumber());
    }

    // ============================================================
    // FormatterStatus
    // ============================================================

    @Test
    void shouldCreateFormatterStatus() {
        FormatterStatus fs = new FormatterStatus();
        fs.setName("prettier");
        fs.setEnabled(true);
        fs.setCommand("npx prettier");
        assertEquals("prettier", fs.getName());
        assertTrue(fs.getEnabled());
    }

    // ============================================================
    // HealthStatus
    // ============================================================

    @Test
    void shouldCreateHealthStatus() {
        HealthStatus hs = new HealthStatus();
        hs.setHealthy(true);
        hs.setVersion("1.0.0");
        assertTrue(hs.getHealthy());
        assertEquals("1.0.0", hs.getVersion());
    }

    // ============================================================
    // LspStatus
    // ============================================================

    @Test
    void shouldCreateLspStatus() {
        LspStatus lsp = new LspStatus();
        lsp.setId("jdtls");
        lsp.setName("Eclipse JDT");
        lsp.setRoot("/project");
        lsp.setStatus("running");
        lsp.setDiagnostics(List.of());
        assertEquals("jdtls", lsp.getId());
        assertEquals("running", lsp.getStatus());
    }

    // ============================================================
    // McpStatus
    // ============================================================

    @Test
    void shouldCreateMcpStatus() {
        McpStatus mcp = new McpStatus();
        mcp.setName("github");
        mcp.setStatus("connected");
        mcp.setConfig(Map.of("url", "http://localhost"));
        mcp.setTools(List.of("tool1"));
        assertEquals("github", mcp.getName());
        assertEquals("connected", mcp.getStatus());
        assertEquals(1, mcp.getTools().size());
    }

    // ============================================================
    // Message
    // ============================================================

    @Test
    void shouldCreateMessage() {
        Message msg = new Message();
        msg.setId("msg-1");
        msg.setSessionId("sess-1");
        msg.setRole("assistant");
        msg.setCreatedAt("2025-01-01T00:00:00Z");
        msg.setUpdatedAt("2025-01-01T00:00:01Z");
        assertEquals("msg-1", msg.getId());
        assertEquals("sess-1", msg.getSessionId());
        assertEquals("assistant", msg.getRole());
    }

    // ============================================================
    // MessageInfo
    // ============================================================

    @Test
    void shouldCreateMessageInfo() {
        MessageInfo info = new MessageInfo();
        Message msg = new Message();
        msg.setId("msg-1");
        info.setInfo(msg);
        info.setParts(List.of());
        assertEquals("msg-1", info.getInfo().getId());
        assertTrue(info.getParts().isEmpty());
    }

    // ============================================================
    // OpenCodeConfig
    // ============================================================

    @Test
    void shouldCreateOpenCodeConfig() {
        OpenCodeConfig cfg = new OpenCodeConfig();
        cfg.setTheme("dark");
        cfg.setModel("anthropic/claude-sonnet-4-5");
        cfg.setAgent("coder");
        cfg.setProvider("anthropic");
        cfg.setDefaultAgent("coder");
        cfg.setDefaultModel("claude-sonnet-4-5");
        cfg.setUsername("user");
        cfg.setShare("public");
        cfg.setAutoshare(true);
        cfg.setMode(Map.of("k", "v"));
        cfg.setProvider_(Map.of("k", "v"));
        cfg.setProviders(Map.of("k", "v"));
        cfg.setAgent_(Map.of("k", "v"));
        cfg.setAgents(Map.of("k", "v"));
        cfg.setPermission(Map.of("k", "v"));
        cfg.setTools(Map.of("k", "v"));
        cfg.setExperimental(Map.of("k", "v"));
        cfg.setExtra(Map.of("k", "v"));
        assertEquals("dark", cfg.getTheme());
        assertEquals("anthropic/claude-sonnet-4-5", cfg.getModel());
        assertEquals("coder", cfg.getAgent());
        assertEquals("anthropic", cfg.getProvider());
        assertEquals("coder", cfg.getDefaultAgent());
        assertEquals("claude-sonnet-4-5", cfg.getDefaultModel());
        assertEquals("user", cfg.getUsername());
        assertEquals("public", cfg.getShare());
        assertTrue(cfg.getAutoshare());
        assertNotNull(cfg.getMode());
        assertNotNull(cfg.getProvider_());
        assertNotNull(cfg.getProviders());
        assertNotNull(cfg.getAgent_());
        assertNotNull(cfg.getAgents());
        assertNotNull(cfg.getPermission());
        assertNotNull(cfg.getTools());
        assertNotNull(cfg.getExperimental());
        assertNotNull(cfg.getExtra());
    }

    // ============================================================
    // OpenCodePath
    // ============================================================

    @Test
    void shouldCreateOpenCodePath() {
        OpenCodePath path = new OpenCodePath();
        path.setHome("/home/user");
        path.setState("/home/user/.opencode/state");
        path.setConfig("/home/user/.opencode/config");
        path.setDirectory("/project");
        path.setWorktree("/project");
        path.setWorktreeDir("/project/.opencode");
        assertEquals("/home/user", path.getHome());
        assertEquals("/project", path.getDirectory());
    }

    // ============================================================
    // Part
    // ============================================================

    @Test
    void shouldCreatePart() {
        Part part = new Part();
        part.setType("text");
        part.setText("hello");
        part.setName("bash");
        part.setToolUseId("call-1");
        assertEquals("text", part.getType());
        assertEquals("hello", part.getText());
        assertEquals("bash", part.getName());
        assertEquals("call-1", part.getToolUseId());
    }

    // ============================================================
    // PermissionRequest
    // ============================================================

    @Test
    void shouldCreatePermissionRequest() {
        PermissionRequest pr = new PermissionRequest();
        pr.setId("perm-1");
        pr.setSessionID("sess-1");
        pr.setPermission("bash");
        pr.setDescription("run ls");
        pr.setMetadata(Map.of("command", "ls"));
        pr.setPatterns(List.of("ls *"));
        assertEquals("perm-1", pr.getId());
        assertEquals("bash", pr.getPermission());
        assertEquals(1, pr.getPatterns().size());
    }

    // ============================================================
    // Project
    // ============================================================

    @Test
    void shouldCreateProject() {
        Project project = new Project();
        project.setId("proj-1");
        project.setName("my-project");
        project.setIcon("icon");
        project.setWorktree("/project");
        project.setVcsDir("/project/.git");
        project.setVcs("git");
        project.setSandboxes(List.of("sandbox1"));
        project.setCreatedAt("2025-01-01");
        assertEquals("proj-1", project.getId());
        assertEquals("my-project", project.getName());
        assertEquals("git", project.getVcs());
    }

    // ============================================================
    // PromptRequest
    // ============================================================

    @Test
    void shouldCreatePromptRequestOfText() {
        PromptRequest req = PromptRequest.ofText("hello");
        assertNotNull(req.getParts());
        assertEquals(1, req.getParts().size());
        assertEquals("text", req.getParts().get(0).getType());
        assertEquals("hello", req.getParts().get(0).getText());
        assertNull(req.getModel());
    }

    @Test
    void shouldCreatePromptRequestOfTextWithModel() {
        PromptRequest req = PromptRequest.ofText("hello", "anthropic", "claude-sonnet-4-5");
        assertNotNull(req.getModel());
        assertEquals("anthropic", req.getModel().getProviderID());
        assertEquals("claude-sonnet-4-5", req.getModel().getModelID());
    }

    @Test
    void shouldSetAllPromptRequestFields() {
        PromptRequest req = new PromptRequest();
        req.setParts(List.of());
        req.setModel(new PromptRequest.ModelRef("anthropic", "claude-sonnet-4-5"));
        req.setAgent("coder");
        req.setNoReply(true);
        req.setSystem("sys");
        assertEquals("coder", req.getAgent());
        assertTrue(req.getNoReply());
        assertEquals("sys", req.getSystem());
    }

    // ============================================================
    // PromptResult
    // ============================================================

    @Test
    void shouldExtractTextContent() {
        Part textPart = new Part();
        textPart.setType("text");
        textPart.setText("hello ");
        Part textPart2 = new Part();
        textPart2.setType("text");
        textPart2.setText("world");
        Part toolPart = new Part();
        toolPart.setType("tool_use");
        toolPart.setText("ignored");

        PromptResult result = new PromptResult();
        result.setParts(List.of(textPart, textPart2, toolPart));
        assertEquals("hello world", result.getTextContent());
    }

    @Test
    void shouldReturnEmptyStringWhenPartsIsNull() {
        PromptResult result = new PromptResult();
        assertEquals("", result.getTextContent());
    }

    @Test
    void shouldReturnEmptyStringWhenNoTextParts() {
        Part toolPart = new Part();
        toolPart.setType("tool_use");
        toolPart.setText("data");
        PromptResult result = new PromptResult();
        result.setParts(List.of(toolPart));
        assertEquals("", result.getTextContent());
    }

    // ============================================================
    // Provider
    // ============================================================

    @Test
    void shouldCreateProvider() {
        Provider p = new Provider();
        p.setId("anthropic");
        p.setName("Anthropic");
        p.setDescription("AI provider");
        p.setSource("builtin");
        p.setAuthMethods(List.of());
        p.setModels(Map.of());
        p.setOptions(Map.of());
        assertEquals("anthropic", p.getId());
        assertEquals("Anthropic", p.getName());
    }

    // ============================================================
    // ProviderAuthAuthorization
    // ============================================================

    @Test
    void shouldCreateProviderAuthAuthorization() {
        ProviderAuthAuthorization auth = new ProviderAuthAuthorization();
        auth.setUrl("https://auth.example.com");
        auth.setMethod("oauth");
        auth.setAuthorizationCode("code-123");
        auth.setState("state-456");
        auth.setInstructions("Follow the link");
        assertEquals("https://auth.example.com", auth.getUrl());
        assertEquals("oauth", auth.getMethod());
        assertEquals("code-123", auth.getAuthorizationCode());
        assertEquals("state-456", auth.getState());
    }

    // ============================================================
    // ProviderAuthMethod
    // ============================================================

    @Test
    void shouldCreateProviderAuthMethod() {
        ProviderAuthMethod method = new ProviderAuthMethod();
        method.setLabel("API Key");
        method.setType("api-key");
        method.setSchema(Map.of("type", "string"));
        method.setPrefill(Map.of());
        method.setPromptOptions(List.of());
        assertEquals("API Key", method.getLabel());
        assertEquals("api-key", method.getType());
    }

    // ============================================================
    // ProviderList
    // ============================================================

    @Test
    void shouldCreateProviderList() {
        ProviderList list = new ProviderList();
        list.setAll(List.of());
        list.setDefaults(Map.of("default", "anthropic/claude-sonnet-4-5"));
        list.setDefault_(Map.of("default", "anthropic/claude-sonnet-4-5"));
        list.setConnected(List.of("anthropic"));
        assertTrue(list.getAll().isEmpty());
        assertEquals(1, list.getConnected().size());
    }

    // ============================================================
    // QuestionRequest
    // ============================================================

    @Test
    void shouldCreateQuestionRequest() {
        QuestionRequest qr = new QuestionRequest();
        qr.setId("q-1");
        qr.setSessionID("sess-1");
        qr.setHeader("Choose");
        qr.setQuestion("Which model?");
        QuestionRequest.QuestionOption opt = new QuestionRequest.QuestionOption();
        opt.setLabel("Option A");
        opt.setDescription("First option");
        opt.setPreview("preview");
        qr.setOptions(List.of(opt));
        assertEquals("q-1", qr.getId());
        assertEquals(1, qr.getOptions().size());
        assertEquals("Option A", qr.getOptions().get(0).getLabel());
    }

    // ============================================================
    // Session
    // ============================================================

    @Test
    void shouldCreateSession() {
        Session s = new Session();
        s.setId("sess-1");
        s.setTitle("my-session");
        s.setParentId(null);
        s.setCreatedAt("2025-01-01");
        s.setUpdatedAt("2025-01-02");
        s.setMetadata(Map.of("key", "val"));
        assertEquals("sess-1", s.getId());
        assertEquals("my-session", s.getTitle());
    }

    // ============================================================
    // SessionStatus
    // ============================================================

    @Test
    void shouldCreateSessionStatus() {
        SessionStatus ss = new SessionStatus();
        ss.setType("idle");
        ss.setMessage("Session is idle");
        assertEquals("idle", ss.getType());
        assertEquals("Session is idle", ss.getMessage());
    }

    // ============================================================
    // SessionTodo
    // ============================================================

    @Test
    void shouldCreateSessionTodo() {
        SessionTodo todo = new SessionTodo();
        todo.setId("todo-1");
        todo.setContent("Fix bug");
        todo.setStatus("pending");
        todo.setPriority("high");
        assertEquals("todo-1", todo.getId());
        assertEquals("Fix bug", todo.getContent());
        assertEquals("pending", todo.getStatus());
        assertEquals("high", todo.getPriority());
    }

    // ============================================================
    // Skill
    // ============================================================

    @Test
    void shouldCreateSkill() {
        Skill skill = new Skill();
        skill.setName("code-review");
        skill.setDescription("Reviews code");
        skill.setLocation("/skills/code-review");
        skill.setContent("...");
        assertEquals("code-review", skill.getName());
        assertEquals("Reviews code", skill.getDescription());
    }

    // ============================================================
    // Symbol
    // ============================================================

    @Test
    void shouldCreateSymbol() {
        Symbol sym = new Symbol();
        sym.setName("OpenCodeClient");
        sym.setKind("class");
        sym.setContainerName("io.github.easy4j.opencode");
        sym.setLocation("OpenCodeClient.java:50");
        sym.setUri("file:///project/OpenCodeClient.java");
        sym.setRange(Map.of());
        assertEquals("OpenCodeClient", sym.getName());
        assertEquals("class", sym.getKind());
    }

    // ============================================================
    // VcsInfo
    // ============================================================

    @Test
    void shouldCreateVcsInfo() {
        VcsInfo vcs = new VcsInfo();
        vcs.setBranch("main");
        vcs.setDefaultBranch("main");
        vcs.setDirty(true);
        vcs.setAhead(2);
        vcs.setBehind(0);
        assertEquals("main", vcs.getBranch());
        assertEquals("main", vcs.getDefaultBranch());
        assertTrue(vcs.getDirty());
        assertEquals(2, vcs.getAhead());
        assertEquals(0, vcs.getBehind());
    }
}
