package io.github.hiwepy.opencode.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hiwepy.opencode.OpenCodeClientConfig;
import io.github.hiwepy.opencode.exception.OpenCodeHttpException;
import io.github.hiwepy.opencode.model.*;
import kong.unirest.core.*;
import kong.unirest.modules.jackson.JacksonObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * OpenCode Server HTTP 客户端，封装所有 REST API。
 *
 * @see <a href="https://opencode.ai/docs/server/">opencode server docs</a>
 */
public class OpenCodeHttpClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(OpenCodeHttpClient.class);

    private final OpenCodeClientConfig config;
    private final UnirestInstance unirest;

    public OpenCodeHttpClient(OpenCodeClientConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.unirest = new UnirestInstance(new Config()
                .connectTimeout(config.getConnectTimeoutMillis())
                .requestTimeout(config.getReadTimeoutMillis())
                .verifySsl(config.isVerifySsl())
                .setObjectMapper(new JacksonObjectMapper(mapper)));

        String password = config.resolvePassword();
        if (!password.isEmpty()) {
            this.unirest.config().setDefaultBasicAuth(config.getUsername(), password);
        }
    }

    // ============================================================
    // Global
    // ============================================================

    public HealthStatus health() {
        return get("/global/health", HealthStatus.class);
    }

    public SimpleResponse dispose() {
        return post("/global/dispose", null, SimpleResponse.class);
    }

    public SimpleResponse upgrade() {
        return post("/global/upgrade", null, SimpleResponse.class);
    }

    public ConfigData getGlobalConfig() {
        return get("/global/config", ConfigData.class);
    }

    public ConfigData updateGlobalConfig(ConfigData config) {
        return patch("/global/config", config, ConfigData.class);
    }

    // ============================================================
    // Config
    // ============================================================

    public ConfigData getConfig() {
        return get("/config", ConfigData.class);
    }

    public ConfigData updateConfig(ConfigData config) {
        return patch("/config", config, ConfigData.class);
    }

    @SuppressWarnings("unchecked")
    public List<Provider> getConfigProviders() {
        return getList("/config/providers", new GenericType<List<Provider>>() {});
    }

    // ============================================================
    // Session
    // ============================================================

    public Session createSession(String title) {
        Map<String, Object> body = title != null ? Map.of("title", title) : Map.of();
        return post("/session", body, Session.class);
    }

    public Session createSession(SessionCreateRequest request) {
        return post("/session", request, Session.class);
    }

    public Session getSession(String sessionId) {
        return get("/session/" + sessionId, Session.class);
    }

    public Session updateSession(String sessionId, SessionUpdateRequest request) {
        return patch("/session/" + sessionId, request, Session.class);
    }

    public List<Session> listSessions() {
        return getList("/session", new GenericType<List<Session>>() {});
    }

    public SessionStatus getSessionStatus() {
        return get("/session/status", SessionStatus.class);
    }

    public boolean deleteSession(String sessionId) {
        HttpResponse<String> resp = unirest.delete(url("/session/" + sessionId)).asString();
        return resp.isSuccess();
    }

    public List<Session> getSessionChildren(String sessionId) {
        return getList("/session/" + sessionId + "/children", new GenericType<List<Session>>() {});
    }

    public List<Todo> getSessionTodos(String sessionId) {
        return getList("/session/" + sessionId + "/todo", new GenericType<List<Todo>>() {});
    }

    public MessageDiff getSessionDiff(String sessionId) {
        return get("/session/" + sessionId + "/diff", MessageDiff.class);
    }

    public Session forkSession(String sessionId) {
        return post("/session/" + sessionId + "/fork", null, Session.class);
    }

    public Session forkSession(String sessionId, Map<String, Object> options) {
        return post("/session/" + sessionId + "/fork", options, Session.class);
    }

    public SimpleResponse initSession(String sessionId) {
        return post("/session/" + sessionId + "/init", null, SimpleResponse.class);
    }

    public ShareInfo shareSession(String sessionId) {
        return post("/session/" + sessionId + "/share", null, ShareInfo.class);
    }

    public SimpleResponse unshareSession(String sessionId) {
        return delete("/session/" + sessionId + "/share", SimpleResponse.class);
    }

    public SimpleResponse summarizeSession(String sessionId) {
        return post("/session/" + sessionId + "/summarize", null, SimpleResponse.class);
    }

    // ============================================================
    // Message / Prompt
    // ============================================================

    public PromptResult prompt(String sessionId, PromptRequest request) {
        return post("/session/" + sessionId + "/message", request, PromptResult.class);
    }

    public boolean promptAsync(String sessionId, PromptRequest request) {
        HttpResponse<String> resp = unirest.post(url("/session/" + sessionId + "/prompt_async"))
                .header("Content-Type", "application/json")
                .body(request)
                .asString();
        return resp.isSuccess();
    }

    public List<PromptResult> getMessages(String sessionId) {
        return getList("/session/" + sessionId + "/message", new GenericType<List<PromptResult>>() {});
    }

    public PromptResult getMessage(String sessionId, String messageId) {
        return get("/session/" + sessionId + "/message/" + messageId, PromptResult.class);
    }

    public boolean deleteMessage(String sessionId, String messageId) {
        HttpResponse<String> resp = unirest.delete(url("/session/" + sessionId + "/message/" + messageId)).asString();
        return resp.isSuccess();
    }

    public boolean abortSession(String sessionId) {
        HttpResponse<String> resp = unirest.post(url("/session/" + sessionId + "/abort")).asString();
        return resp.isSuccess();
    }

    public SimpleResponse revertMessage(String sessionId) {
        return post("/session/" + sessionId + "/revert", null, SimpleResponse.class);
    }

    public SimpleResponse unrevertMessage(String sessionId) {
        return post("/session/" + sessionId + "/unrevert", null, SimpleResponse.class);
    }

    public boolean deletePart(String sessionId, String messageId, String partId) {
        HttpResponse<String> resp = unirest.delete(url("/session/" + sessionId + "/message/" + messageId + "/part/" + partId)).asString();
        return resp.isSuccess();
    }

    public SimpleResponse updatePart(String sessionId, String messageId, String partId, Object body) {
        return patch("/session/" + sessionId + "/message/" + messageId + "/part/" + partId, body, SimpleResponse.class);
    }

    // ============================================================
    // Session Commands
    // ============================================================

    public ShellResult executeCommand(String sessionId, String command) {
        Map<String, Object> body = Map.of("command", command);
        return post("/session/" + sessionId + "/command", body, ShellResult.class);
    }

    public ShellResult runShell(String sessionId, ShellRequest request) {
        return post("/session/" + sessionId + "/shell", request, ShellResult.class);
    }

    // ============================================================
    // Agent
    // ============================================================

    public List<Agent> listAgents() {
        return getList("/agent", new GenericType<List<Agent>>() {});
    }

    // ============================================================
    // Command / Skill
    // ============================================================

    public List<Command> listCommands() {
        return getList("/command", new GenericType<List<Command>>() {});
    }

    public List<Skill> listSkills() {
        return getList("/skill", new GenericType<List<Skill>>() {});
    }

    // ============================================================
    // Permission
    // ============================================================

    public List<PermissionRequest> listPermissions() {
        return getList("/permission", new GenericType<List<PermissionRequest>>() {});
    }

    public SimpleResponse replyToPermission(String requestId, PermissionReply reply) {
        return post("/permission/" + requestId + "/reply", reply, SimpleResponse.class);
    }

    // ============================================================
    // Question
    // ============================================================

    public List<Question> listQuestions() {
        return getList("/question", new GenericType<List<Question>>() {});
    }

    public SimpleResponse replyToQuestion(String requestId, QuestionReply reply) {
        return post("/question/" + requestId + "/reply", reply, SimpleResponse.class);
    }

    public SimpleResponse rejectQuestion(String requestId) {
        return post("/question/" + requestId + "/reject", null, SimpleResponse.class);
    }

    // ============================================================
    // Provider
    // ============================================================

    public List<Provider> listProviders() {
        return getList("/provider", new GenericType<List<Provider>>() {});
    }

    public List<ProviderAuth> getProviderAuth() {
        return getList("/provider/auth", new GenericType<List<ProviderAuth>>() {});
    }

    // ============================================================
    // Auth
    // ============================================================

    public SimpleResponse setAuth(String providerId, AuthCredential credential) {
        return put("/auth/" + providerId, credential, SimpleResponse.class);
    }

    public SimpleResponse removeAuth(String providerId) {
        return delete("/auth/" + providerId, SimpleResponse.class);
    }

    public OAuthResponse startOAuth(String providerId) {
        return post("/provider/" + providerId + "/oauth/authorize", null, OAuthResponse.class);
    }

    public OAuthResponse completeOAuth(String providerId, Map<String, Object> callbackParams) {
        return post("/provider/" + providerId + "/oauth/callback", callbackParams, OAuthResponse.class);
    }

    // ============================================================
    // Log
    // ============================================================

    public SimpleResponse writeLog(LogEntry entry) {
        return post("/log", entry, SimpleResponse.class);
    }

    // ============================================================
    // File / Find
    // ============================================================

    @SuppressWarnings("unchecked")
    public List<FileInfo> listFiles(String directory) {
        HttpRequest req = unirest.get(url("/file"));
        if (directory != null) req.queryString("directory", directory);
        HttpResponse<Object> resp = req.asObject(new GenericType<List<FileInfo>>() {});
        checkSuccess(resp);
        return (List<FileInfo>) resp.getBody();
    }

    public FileContent getFileContent(String path) {
        return getWithQuery("/file/content", Map.of("path", path), FileContent.class);
    }

    public FileStatus getFileStatus(String path) {
        return getWithQuery("/file/status", Map.of("path", path), FileStatus.class);
    }

    @SuppressWarnings("unchecked")
    public List<FindResult> find(String query, String directory, String filePattern) {
        HttpRequest req = unirest.get(url("/find"))
                .queryString("query", query);
        if (directory != null) req.queryString("directory", directory);
        if (filePattern != null) req.queryString("pattern", filePattern);
        HttpResponse<Object> resp = req.asObject(new GenericType<List<FindResult>>() {});
        checkSuccess(resp);
        return (List<FindResult>) resp.getBody();
    }

    @SuppressWarnings("unchecked")
    public List<FindResult> findFile(String pattern, String directory) {
        HttpRequest req = unirest.get(url("/find/file"))
                .queryString("pattern", pattern);
        if (directory != null) req.queryString("directory", directory);
        HttpResponse<Object> resp = req.asObject(new GenericType<List<FindResult>>() {});
        checkSuccess(resp);
        return (List<FindResult>) resp.getBody();
    }

    @SuppressWarnings("unchecked")
    public List<LspSymbol> findSymbol(String query, String directory) {
        HttpRequest req = unirest.get(url("/find/symbol"))
                .queryString("query", query);
        if (directory != null) req.queryString("directory", directory);
        HttpResponse<Object> resp = req.asObject(new GenericType<List<LspSymbol>>() {});
        checkSuccess(resp);
        return (List<LspSymbol>) resp.getBody();
    }

    public FormatterStatus getFormatter() {
        return get("/formatter", FormatterStatus.class);
    }

    public Map<String, Object> getLspStatus() {
        return get("/lsp", new GenericType<Map<String, Object>>() {});
    }

    public PathInfo getPathInfo() {
        return get("/path", PathInfo.class);
    }

    // ============================================================
    // Project
    // ============================================================

    public List<Project> listProjects() {
        return getList("/project", new GenericType<List<Project>>() {});
    }

    public Project getCurrentProject() {
        return get("/project/current", Project.class);
    }

    public SimpleResponse initGitProject() {
        return post("/project/git/init", null, SimpleResponse.class);
    }

    public Project updateProject(String projectId, Map<String, Object> update) {
        return patch("/project/" + projectId, update, Project.class);
    }

    public List<FileInfo> listProjectDirectories(String projectId) {
        return getList("/project/" + projectId + "/directories", new GenericType<List<FileInfo>>() {});
    }

    // ============================================================
    // PTY
    // ============================================================

    public List<PtySession> listPtys() {
        return getList("/pty", new GenericType<List<PtySession>>() {});
    }

    public PtySession createPty(Map<String, Object> params) {
        return post("/pty", params, PtySession.class);
    }

    public PtyShellInfo getPtyShells() {
        return get("/pty/shells", PtyShellInfo.class);
    }

    public PtySession getPty(String ptyId) {
        return get("/pty/" + ptyId, PtySession.class);
    }

    public PtySession updatePty(String ptyId, Map<String, Object> params) {
        return put("/pty/" + ptyId, params, PtySession.class);
    }

    public SimpleResponse deletePty(String ptyId) {
        return delete("/pty/" + ptyId, SimpleResponse.class);
    }

    public Map<String, Object> createPtyConnectToken(String ptyId) {
        return post("/pty/" + ptyId + "/connect-token", null, new GenericType<Map<String, Object>>() {});
    }

    // ============================================================
    // VCS
    // ============================================================

    public Map<String, Object> getVcsInfo() {
        return get("/vcs", new GenericType<Map<String, Object>>() {});
    }

    public VcsStatus getVcsStatus() {
        return get("/vcs/status", VcsStatus.class);
    }

    public VcsDiff getVcsDiff() {
        return get("/vcs/diff", VcsDiff.class);
    }

    public String getVcsDiffRaw() {
        HttpResponse<String> resp = unirest.get(url("/vcs/diff/raw")).asString();
        checkSuccess(resp);
        return resp.getBody();
    }

    public SimpleResponse applyPatch(PatchRequest request) {
        return post("/vcs/apply", request, SimpleResponse.class);
    }

    // ============================================================
    // MCP
    // ============================================================

    public List<McpServer> getMcpStatus() {
        return getList("/mcp", new GenericType<List<McpServer>>() {});
    }

    public McpServer addMcpServer(McpServerConfig config) {
        return post("/mcp", config, McpServer.class);
    }

    public SimpleResponse connectMcpServer(String name) {
        return post("/mcp/" + name + "/connect", null, SimpleResponse.class);
    }

    public SimpleResponse disconnectMcpServer(String name) {
        return post("/mcp/" + name + "/disconnect", null, SimpleResponse.class);
    }

    public SimpleResponse removeMcpAuth(String name) {
        return delete("/mcp/" + name + "/auth", SimpleResponse.class);
    }

    public OAuthResponse startMcpOAuth(String name) {
        return post("/mcp/" + name + "/auth", null, OAuthResponse.class);
    }

    public OAuthResponse completeMcpOAuth(String name, Map<String, Object> params) {
        return post("/mcp/" + name + "/auth/callback", params, OAuthResponse.class);
    }

    public OAuthResponse authenticateMcp(String name) {
        return post("/mcp/" + name + "/auth/authenticate", null, OAuthResponse.class);
    }

    // ============================================================
    // Experimental - Tool
    // ============================================================

    @SuppressWarnings("unchecked")
    public List<ToolInfo> listTools(String providerId, String modelId) {
        HttpRequest req = unirest.get(url("/experimental/tool"));
        if (providerId != null) req.queryString("providerID", providerId);
        if (modelId != null) req.queryString("modelID", modelId);
        HttpResponse<Object> resp = req.asObject(new GenericType<List<ToolInfo>>() {});
        checkSuccess(resp);
        return (List<ToolInfo>) resp.getBody();
    }

    public List<String> listToolIds() {
        return getList("/experimental/tool/ids", new GenericType<List<String>>() {});
    }

    // ============================================================
    // Experimental - Worktree
    // ============================================================

    public List<Worktree> listWorktrees() {
        return getList("/experimental/worktree", new GenericType<List<Worktree>>() {});
    }

    public Worktree createWorktree(Map<String, Object> params) {
        return post("/experimental/worktree", params, Worktree.class);
    }

    public SimpleResponse deleteWorktree(Map<String, Object> params) {
        HttpRequest req = unirest.delete(url("/experimental/worktree"))
                .header("Content-Type", "application/json");
        if (params != null) {
            params.forEach((k, v) -> { if (v != null) req.queryString(k, v); });
        }
        HttpResponse<SimpleResponse> resp = req.asObject(SimpleResponse.class);
        checkSuccess(resp);
        return resp.getBody();
    }

    public SimpleResponse resetWorktree(String name) {
        return post("/experimental/worktree/reset", Map.of("name", name), SimpleResponse.class);
    }

    // ============================================================
    // Experimental - Session
    // ============================================================

    public ApiResponse<List<Session>> listSessionsV2(Map<String, Object> params) {
        return getApiResponse("/experimental/session", params, new GenericType<List<Session>>() {});
    }

    public SimpleResponse detachBackgroundSubagents(String sessionId) {
        return post("/experimental/session/" + sessionId + "/background", null, SimpleResponse.class);
    }

    // ============================================================
    // Experimental - Resource / Console
    // ============================================================

    @SuppressWarnings("unchecked")
    public List<McpResource> listResources(String serverName) {
        HttpRequest req = unirest.get(url("/experimental/resource"));
        if (serverName != null) req.queryString("server", serverName);
        HttpResponse<Object> resp = req.asObject(new GenericType<List<McpResource>>() {});
        checkSuccess(resp);
        return (List<McpResource>) resp.getBody();
    }

    public Map<String, Object> getConsoleMetadata() {
        return get("/experimental/console", new GenericType<Map<String, Object>>() {});
    }

    public List<Map<String, Object>> getConsoleOrgs() {
        return getList("/experimental/console/orgs", new GenericType<List<Map<String, Object>>>() {});
    }

    public SimpleResponse switchConsoleOrg(String orgId) {
        return post("/experimental/console/switch", Map.of("orgID", orgId), SimpleResponse.class);
    }

    // ============================================================
    // Experimental - Workspace
    // ============================================================

    public List<Workspace> listWorkspaces() {
        return getList("/experimental/workspace", new GenericType<List<Workspace>>() {});
    }

    public Workspace createWorkspace(Map<String, Object> params) {
        return post("/experimental/workspace", params, Workspace.class);
    }

    public List<Map<String, Object>> listWorkspaceAdapters() {
        return getList("/experimental/workspace/adapter", new GenericType<List<Map<String, Object>>>() {});
    }

    public SimpleResponse syncWorkspaceList() {
        return post("/experimental/workspace/sync-list", null, SimpleResponse.class);
    }

    public Map<String, Object> getWorkspaceStatus() {
        return get("/experimental/workspace/status", new GenericType<Map<String, Object>>() {});
    }

    public SimpleResponse deleteWorkspace(String id) {
        return delete("/experimental/workspace/" + id, SimpleResponse.class);
    }

    public SimpleResponse warpToWorkspace(String sessionId, String workspaceId) {
        return post("/experimental/workspace/warp",
                Map.of("sessionID", sessionId, "workspaceID", workspaceId), SimpleResponse.class);
    }

    // ============================================================
    // Experimental - Control Plane / Project Copy
    // ============================================================

    public SimpleResponse moveSession(String sessionId, String targetProjectId) {
        return post("/experimental/control-plane/move-session",
                Map.of("sessionID", sessionId, "targetProjectID", targetProjectId), SimpleResponse.class);
    }

    public SimpleResponse copyProject(String projectId) {
        return post("/experimental/project/" + projectId + "/copy", null, SimpleResponse.class);
    }

    public SimpleResponse deleteProjectCopy(String projectId) {
        return delete("/experimental/project/" + projectId + "/copy", SimpleResponse.class);
    }

    public SimpleResponse refreshProjectCopies(String projectId) {
        return post("/experimental/project/" + projectId + "/copy/refresh", null, SimpleResponse.class);
    }

    // ============================================================
    // Sync
    // ============================================================

    public SimpleResponse startSync(Map<String, Object> params) {
        return post("/sync/start", params, SimpleResponse.class);
    }

    public SimpleResponse replaySync(Map<String, Object> params) {
        return post("/sync/replay", params, SimpleResponse.class);
    }

    public SimpleResponse stealSession(Map<String, Object> params) {
        return post("/sync/steal", params, SimpleResponse.class);
    }

    public List<Map<String, Object>> getSyncHistory(Map<String, Object> params) {
        return postList("/sync/history", params, new GenericType<List<Map<String, Object>>>() {});
    }

    // ============================================================
    // TUI
    // ============================================================

    public SimpleResponse tuiAppendPrompt(String prompt) {
        return post("/tui/append-prompt", Map.of("prompt", prompt), SimpleResponse.class);
    }

    public SimpleResponse tuiOpenHelp() {
        return post("/tui/open-help", null, SimpleResponse.class);
    }

    public SimpleResponse tuiOpenSessions() {
        return post("/tui/open-sessions", null, SimpleResponse.class);
    }

    public SimpleResponse tuiOpenThemes() {
        return post("/tui/open-themes", null, SimpleResponse.class);
    }

    public SimpleResponse tuiOpenModels() {
        return post("/tui/open-models", null, SimpleResponse.class);
    }

    public SimpleResponse tuiSubmitPrompt(String prompt) {
        return post("/tui/submit-prompt", Map.of("prompt", prompt), SimpleResponse.class);
    }

    public SimpleResponse tuiClearPrompt() {
        return post("/tui/clear-prompt", null, SimpleResponse.class);
    }

    public SimpleResponse tuiExecuteCommand(String command) {
        return post("/tui/execute-command", Map.of("command", command), SimpleResponse.class);
    }

    public SimpleResponse tuiShowToast(String message, String type) {
        return post("/tui/show-toast", Map.of("message", message, "type", type), SimpleResponse.class);
    }

    public SimpleResponse tuiPublish(String event, Map<String, Object> data) {
        return post("/tui/publish", Map.of("event", event, "data", data), SimpleResponse.class);
    }

    public SimpleResponse tuiSelectSession(String sessionId) {
        return post("/tui/select-session", Map.of("sessionID", sessionId), SimpleResponse.class);
    }

    // ============================================================
    // Instance
    // ============================================================

    public SimpleResponse disposeInstance() {
        return post("/instance/dispose", null, SimpleResponse.class);
    }

    // ============================================================
    // V2 API
    // ============================================================

    public HealthStatus healthV2() {
        return get("/api/health", HealthStatus.class);
    }

    public List<Agent> listAgentsV2() {
        return getList("/api/agent", new GenericType<List<Agent>>() {});
    }

    public ApiResponse<List<Session>> listSessionsApi(Map<String, Object> params) {
        return getApiResponse("/api/session", params, new GenericType<List<Session>>() {});
    }

    public ApiResponse<List<ModelInfo>> listModelsApi() {
        return get("/api/model", new GenericType<ApiResponse<List<ModelInfo>>>() {});
    }

    public List<Provider> listProvidersApi() {
        return getList("/api/provider", new GenericType<List<Provider>>() {});
    }

    public Provider getProviderApi(String providerId) {
        return get("/api/provider/" + providerId, Provider.class);
    }

    // ============================================================
    // Internal helpers
    // ============================================================

    private String url(String path) {
        return config.getServerUrl() + path;
    }

    private <T> T get(String path, Class<T> type) {
        HttpResponse<T> resp = unirest.get(url(path)).asObject(type);
        checkSuccess(resp);
        return resp.getBody();
    }

    private <T> T get(String path, GenericType<T> genericType) {
        HttpResponse<T> resp = unirest.get(url(path)).asObject(genericType);
        checkSuccess(resp);
        return resp.getBody();
    }

    @SuppressWarnings("unchecked")
    private <T> T getWithQuery(String path, Map<String, Object> params, Class<T> type) {
        HttpRequest req = unirest.get(url(path));
        params.forEach((k, v) -> { if (v != null) req.queryString(k, v); });
        if (type == Map.class || type.isAssignableFrom(Map.class)) {
            HttpResponse<T> resp = req.asObject((GenericType<T>) new GenericType<Map<String, Object>>() {});
            checkSuccess(resp);
            return resp.getBody();
        }
        HttpResponse<T> resp = req.asObject(type);
        checkSuccess(resp);
        return resp.getBody();
    }

    private <T> T getList(String path, GenericType<T> genericType) {
        HttpResponse<T> resp = unirest.get(url(path)).asObject(genericType);
        checkSuccess(resp);
        return resp.getBody();
    }

    private <T> T post(String path, Object body, Class<T> type) {
        HttpRequestWithBody req = unirest.post(url(path)).header("Content-Type", "application/json");
        if (body != null) req.body(body);
        HttpResponse<T> resp = req.asObject(type);
        checkSuccess(resp);
        return resp.getBody();
    }

    private <T> T post(String path, Object body, GenericType<T> genericType) {
        HttpRequestWithBody req = unirest.post(url(path)).header("Content-Type", "application/json");
        if (body != null) req.body(body);
        HttpResponse<T> resp = req.asObject(genericType);
        checkSuccess(resp);
        return resp.getBody();
    }

    private <T> T postList(String path, Object body, GenericType<T> genericType) {
        return post(path, body, genericType);
    }

    private <T> T patch(String path, Object body, Class<T> type) {
        HttpRequestWithBody req = unirest.patch(url(path)).header("Content-Type", "application/json");
        if (body != null) req.body(body);
        HttpResponse<T> resp = req.asObject(type);
        checkSuccess(resp);
        return resp.getBody();
    }

    private <T> T put(String path, Object body, Class<T> type) {
        HttpRequestWithBody req = unirest.put(url(path)).header("Content-Type", "application/json");
        if (body != null) req.body(body);
        HttpResponse<T> resp = req.asObject(type);
        checkSuccess(resp);
        return resp.getBody();
    }

    private <T> T delete(String path, Class<T> type) {
        HttpResponse<T> resp = unirest.delete(url(path)).asObject(type);
        checkSuccess(resp);
        return resp.getBody();
    }

    @SuppressWarnings("unchecked")
    private <T> ApiResponse<T> getApiResponse(String path, Map<String, Object> params, GenericType<T> innerType) {
        HttpRequest req = unirest.get(url(path));
        if (params != null) params.forEach((k, v) -> { if (v != null) req.queryString(k, v); });
        HttpResponse<Object> resp = req.asObject(Object.class);
        checkSuccess(resp);
        Object body = resp.getBody();
        if (body == null) return new ApiResponse<>(null, null);
        try {
            ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            String json = mapper.writeValueAsString(body);
            JavaType innerJavaType = mapper.getTypeFactory().constructType(innerType.getType());
            JavaType apiResponseType = mapper.getTypeFactory().constructParametricType(ApiResponse.class, innerJavaType);
            return mapper.readValue(json, apiResponseType);
        } catch (Exception e) {
            log.warn("Failed to parse ApiResponse: {}", e.getMessage());
            return new ApiResponse<>(null, null);
        }
    }

    private void checkSuccess(HttpResponse<?> resp) {
        if (!resp.isSuccess()) {
            String respBody = resp.getBody() != null ? resp.getBody().toString() : "";
            throw new OpenCodeHttpException(resp.getStatus(), respBody);
        }
    }

    @Override
    public void close() {
        unirest.close();
    }
}
