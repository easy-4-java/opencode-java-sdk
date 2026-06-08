package io.github.hiwepy.opencode;

import io.github.hiwepy.opencode.cli.OpenCodeCli;
import io.github.hiwepy.opencode.cli.OpenCodeCliExecutor;
import io.github.hiwepy.opencode.http.OpenCodeHttpClient;
import io.github.hiwepy.opencode.http.OpenCodeSseClient;
import io.github.hiwepy.opencode.model.*;
import io.github.hiwepy.opencode.model.event.TypedEvent;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

/**
 * OpenCode 客户端门面：HTTP Server + SSE 事件流 + 本地 CLI。
 * <p>
 * 完整覆盖 OpenCode Server 的所有 REST API、SSE 事件流和 CLI 命令。
 * </p>
 */
public class OpenCodeClient implements AutoCloseable {

    private final OpenCodeClientConfig config;
    private final OpenCodeHttpClient httpClient;
    private final OpenCodeSseClient sseClient;
    private final OpenCodeCli cli;

    public OpenCodeClient(OpenCodeClientConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.httpClient = new OpenCodeHttpClient(config);
        this.sseClient = new OpenCodeSseClient(config);
        this.cli = new OpenCodeCli(new OpenCodeCliExecutor(config));
    }

    public OpenCodeClient(OpenCodeClientConfig config,
                          OpenCodeHttpClient httpClient,
                          OpenCodeSseClient sseClient,
                          OpenCodeCli cli) {
        this.config = Objects.requireNonNull(config, "config");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.sseClient = Objects.requireNonNull(sseClient, "sseClient");
        this.cli = Objects.requireNonNull(cli, "cli");
    }

    // ============================================================
    // Global
    // ============================================================

    public HealthStatus health() { return httpClient.health(); }
    public SimpleResponse dispose() { return httpClient.dispose(); }
    public SimpleResponse upgrade() { return httpClient.upgrade(); }
    public ConfigData getGlobalConfig() { return httpClient.getGlobalConfig(); }
    public ConfigData updateGlobalConfig(ConfigData config) { return httpClient.updateGlobalConfig(config); }

    // ============================================================
    // Config
    // ============================================================

    public ConfigData getProjectConfig() { return httpClient.getConfig(); }
    public ConfigData updateProjectConfig(ConfigData config) { return httpClient.updateConfig(config); }
    public List<Provider> getConfigProviders() { return httpClient.getConfigProviders(); }

    // ============================================================
    // Session
    // ============================================================

    public Session createSession(String title) { return httpClient.createSession(title); }
    public Session createSession(SessionCreateRequest request) { return httpClient.createSession(request); }
    public Session getSession(String sessionId) { return httpClient.getSession(sessionId); }
    public Session updateSession(String sessionId, SessionUpdateRequest request) { return httpClient.updateSession(sessionId, request); }
    public List<Session> listSessions() { return httpClient.listSessions(); }
    public SessionStatus getSessionStatus() { return httpClient.getSessionStatus(); }
    public boolean deleteSession(String sessionId) { return httpClient.deleteSession(sessionId); }
    public List<Session> getSessionChildren(String sessionId) { return httpClient.getSessionChildren(sessionId); }
    public List<Todo> getSessionTodos(String sessionId) { return httpClient.getSessionTodos(sessionId); }
    public MessageDiff getSessionDiff(String sessionId) { return httpClient.getSessionDiff(sessionId); }
    public Session forkSession(String sessionId) { return httpClient.forkSession(sessionId); }
    public Session forkSession(String sessionId, Map<String, Object> options) { return httpClient.forkSession(sessionId, options); }
    public SimpleResponse initSession(String sessionId) { return httpClient.initSession(sessionId); }
    public ShareInfo shareSession(String sessionId) { return httpClient.shareSession(sessionId); }
    public SimpleResponse unshareSession(String sessionId) { return httpClient.unshareSession(sessionId); }
    public SimpleResponse summarizeSession(String sessionId) { return httpClient.summarizeSession(sessionId); }

    // ============================================================
    // Prompt
    // ============================================================

    public PromptResult prompt(String sessionId, PromptRequest request) { return httpClient.prompt(sessionId, request); }
    public PromptResult prompt(String sessionId, String text) { return httpClient.prompt(sessionId, PromptRequest.ofText(text)); }
    public PromptResult prompt(String sessionId, String text, String providerID, String modelID) { return httpClient.prompt(sessionId, PromptRequest.ofText(text, providerID, modelID)); }
    public boolean promptAsync(String sessionId, PromptRequest request) { return httpClient.promptAsync(sessionId, request); }
    public boolean promptAsync(String sessionId, String text) { return httpClient.promptAsync(sessionId, PromptRequest.ofText(text)); }
    public List<PromptResult> getMessages(String sessionId) { return httpClient.getMessages(sessionId); }
    public PromptResult getMessage(String sessionId, String messageId) { return httpClient.getMessage(sessionId, messageId); }
    public boolean deleteMessage(String sessionId, String messageId) { return httpClient.deleteMessage(sessionId, messageId); }
    public boolean abort(String sessionId) { return httpClient.abortSession(sessionId); }
    public SimpleResponse revertMessage(String sessionId) { return httpClient.revertMessage(sessionId); }
    public SimpleResponse unrevertMessage(String sessionId) { return httpClient.unrevertMessage(sessionId); }
    public boolean deletePart(String sessionId, String messageId, String partId) { return httpClient.deletePart(sessionId, messageId, partId); }
    public SimpleResponse updatePart(String sessionId, String messageId, String partId, Object body) { return httpClient.updatePart(sessionId, messageId, partId, body); }

    // ============================================================
    // Session Commands
    // ============================================================

    public ShellResult executeCommand(String sessionId, String command) { return httpClient.executeCommand(sessionId, command); }
    public ShellResult runShell(String sessionId, ShellRequest request) { return httpClient.runShell(sessionId, request); }

    // ============================================================
    // Agent
    // ============================================================

    public List<Agent> listAgents() { return httpClient.listAgents(); }

    // ============================================================
    // Command / Skill
    // ============================================================

    public List<Command> listCommands() { return httpClient.listCommands(); }
    public List<Skill> listSkills() { return httpClient.listSkills(); }

    // ============================================================
    // Permission
    // ============================================================

    public List<PermissionRequest> listPermissions() { return httpClient.listPermissions(); }
    public SimpleResponse replyToPermission(String requestId, PermissionReply reply) { return httpClient.replyToPermission(requestId, reply); }
    public SimpleResponse allowPermission(String requestId) { return replyToPermission(requestId, new PermissionReply("once", null)); }
    public SimpleResponse allowPermissionAlways(String requestId) { return replyToPermission(requestId, new PermissionReply("always", null)); }
    public SimpleResponse rejectPermission(String requestId, String message) { return replyToPermission(requestId, new PermissionReply("reject", message)); }

    // ============================================================
    // Question
    // ============================================================

    public List<Question> listQuestions() { return httpClient.listQuestions(); }
    public SimpleResponse replyToQuestion(String requestId, QuestionReply reply) { return httpClient.replyToQuestion(requestId, reply); }
    public SimpleResponse answerQuestion(String requestId, String answer) { return replyToQuestion(requestId, new QuestionReply(answer, null)); }
    public SimpleResponse rejectQuestion(String requestId) { return httpClient.rejectQuestion(requestId); }

    // ============================================================
    // Provider / Auth
    // ============================================================

    public List<Provider> listProviders() { return httpClient.listProviders(); }
    public List<ProviderAuth> getProviderAuth() { return httpClient.getProviderAuth(); }
    public SimpleResponse setAuth(String providerId, AuthCredential credential) { return httpClient.setAuth(providerId, credential); }
    public SimpleResponse removeAuth(String providerId) { return httpClient.removeAuth(providerId); }
    public OAuthResponse startOAuth(String providerId) { return httpClient.startOAuth(providerId); }
    public OAuthResponse completeOAuth(String providerId, Map<String, Object> callbackParams) { return httpClient.completeOAuth(providerId, callbackParams); }

    // ============================================================
    // Log
    // ============================================================

    public SimpleResponse writeLog(LogEntry entry) { return httpClient.writeLog(entry); }

    // ============================================================
    // File / Find
    // ============================================================

    public List<FileInfo> listFiles(String directory) { return httpClient.listFiles(directory); }
    public List<FileInfo> listFiles() { return httpClient.listFiles(null); }
    public FileContent getFileContent(String path) { return httpClient.getFileContent(path); }
    public FileStatus getFileStatus(String path) { return httpClient.getFileStatus(path); }
    public List<FindResult> find(String query, String directory, String filePattern) { return httpClient.find(query, directory, filePattern); }
    public List<FindResult> find(String query) { return httpClient.find(query, null, null); }
    public List<FindResult> findFile(String pattern, String directory) { return httpClient.findFile(pattern, directory); }
    public List<FindResult> findFile(String pattern) { return httpClient.findFile(pattern, null); }
    public List<LspSymbol> findSymbol(String query, String directory) { return httpClient.findSymbol(query, directory); }
    public List<LspSymbol> findSymbol(String query) { return httpClient.findSymbol(query, null); }
    public FormatterStatus getFormatter() { return httpClient.getFormatter(); }
    public Map<String, Object> getLspStatus() { return httpClient.getLspStatus(); }
    public PathInfo getPathInfo() { return httpClient.getPathInfo(); }

    // ============================================================
    // Project
    // ============================================================

    public List<Project> listProjects() { return httpClient.listProjects(); }
    public Project getCurrentProject() { return httpClient.getCurrentProject(); }
    public SimpleResponse initGitProject() { return httpClient.initGitProject(); }
    public Project updateProject(String projectId, Map<String, Object> update) { return httpClient.updateProject(projectId, update); }
    public List<FileInfo> listProjectDirectories(String projectId) { return httpClient.listProjectDirectories(projectId); }

    // ============================================================
    // PTY
    // ============================================================

    public List<PtySession> listPtys() { return httpClient.listPtys(); }
    public PtySession createPty(Map<String, Object> params) { return httpClient.createPty(params); }
    public PtyShellInfo getPtyShells() { return httpClient.getPtyShells(); }
    public PtySession getPty(String ptyId) { return httpClient.getPty(ptyId); }
    public PtySession updatePty(String ptyId, Map<String, Object> params) { return httpClient.updatePty(ptyId, params); }
    public SimpleResponse deletePty(String ptyId) { return httpClient.deletePty(ptyId); }
    public Map<String, Object> createPtyConnectToken(String ptyId) { return httpClient.createPtyConnectToken(ptyId); }

    // ============================================================
    // VCS
    // ============================================================

    public Map<String, Object> getVcsInfo() { return httpClient.getVcsInfo(); }
    public VcsStatus getVcsStatus() { return httpClient.getVcsStatus(); }
    public VcsDiff getVcsDiff() { return httpClient.getVcsDiff(); }
    public String getVcsDiffRaw() { return httpClient.getVcsDiffRaw(); }
    public SimpleResponse applyPatch(PatchRequest request) { return httpClient.applyPatch(request); }

    // ============================================================
    // MCP
    // ============================================================

    public List<McpServer> getMcpStatus() { return httpClient.getMcpStatus(); }
    public McpServer addMcpServer(McpServerConfig config) { return httpClient.addMcpServer(config); }
    public SimpleResponse connectMcpServer(String name) { return httpClient.connectMcpServer(name); }
    public SimpleResponse disconnectMcpServer(String name) { return httpClient.disconnectMcpServer(name); }
    public SimpleResponse removeMcpAuth(String name) { return httpClient.removeMcpAuth(name); }
    public OAuthResponse startMcpOAuth(String name) { return httpClient.startMcpOAuth(name); }
    public OAuthResponse completeMcpOAuth(String name, Map<String, Object> params) { return httpClient.completeMcpOAuth(name, params); }
    public OAuthResponse authenticateMcp(String name) { return httpClient.authenticateMcp(name); }

    // ============================================================
    // Experimental
    // ============================================================

    public List<ToolInfo> listTools(String providerId, String modelId) { return httpClient.listTools(providerId, modelId); }
    public List<ToolInfo> listTools() { return httpClient.listTools(null, null); }
    public List<String> listToolIds() { return httpClient.listToolIds(); }
    public List<Worktree> listWorktrees() { return httpClient.listWorktrees(); }
    public Worktree createWorktree(Map<String, Object> params) { return httpClient.createWorktree(params); }
    public SimpleResponse deleteWorktree(Map<String, Object> params) { return httpClient.deleteWorktree(params); }
    public SimpleResponse resetWorktree(String name) { return httpClient.resetWorktree(name); }
    public ApiResponse<List<Session>> listSessionsV2(Map<String, Object> params) { return httpClient.listSessionsV2(params); }
    public SimpleResponse detachBackgroundSubagents(String sessionId) { return httpClient.detachBackgroundSubagents(sessionId); }
    public List<McpResource> listResources() { return httpClient.listResources(null); }
    public List<McpResource> listResources(String serverName) { return httpClient.listResources(serverName); }
    public Map<String, Object> getConsoleMetadata() { return httpClient.getConsoleMetadata(); }
    public List<Map<String, Object>> getConsoleOrgs() { return httpClient.getConsoleOrgs(); }
    public SimpleResponse switchConsoleOrg(String orgId) { return httpClient.switchConsoleOrg(orgId); }

    // ============================================================
    // Workspace
    // ============================================================

    public List<Workspace> listWorkspaces() { return httpClient.listWorkspaces(); }
    public Workspace createWorkspace(Map<String, Object> params) { return httpClient.createWorkspace(params); }
    public List<Map<String, Object>> listWorkspaceAdapters() { return httpClient.listWorkspaceAdapters(); }
    public SimpleResponse syncWorkspaceList() { return httpClient.syncWorkspaceList(); }
    public Map<String, Object> getWorkspaceStatus() { return httpClient.getWorkspaceStatus(); }
    public SimpleResponse deleteWorkspace(String id) { return httpClient.deleteWorkspace(id); }
    public SimpleResponse warpToWorkspace(String sessionId, String workspaceId) { return httpClient.warpToWorkspace(sessionId, workspaceId); }

    // ============================================================
    // Control Plane / Project Copy
    // ============================================================

    public SimpleResponse moveSession(String sessionId, String targetProjectId) { return httpClient.moveSession(sessionId, targetProjectId); }
    public SimpleResponse copyProject(String projectId) { return httpClient.copyProject(projectId); }
    public SimpleResponse deleteProjectCopy(String projectId) { return httpClient.deleteProjectCopy(projectId); }
    public SimpleResponse refreshProjectCopies(String projectId) { return httpClient.refreshProjectCopies(projectId); }

    // ============================================================
    // Sync
    // ============================================================

    public SimpleResponse startSync(Map<String, Object> params) { return httpClient.startSync(params); }
    public SimpleResponse replaySync(Map<String, Object> params) { return httpClient.replaySync(params); }
    public SimpleResponse stealSession(Map<String, Object> params) { return httpClient.stealSession(params); }
    public List<Map<String, Object>> getSyncHistory(Map<String, Object> params) { return httpClient.getSyncHistory(params); }

    // ============================================================
    // TUI
    // ============================================================

    public SimpleResponse tuiAppendPrompt(String prompt) { return httpClient.tuiAppendPrompt(prompt); }
    public SimpleResponse tuiOpenHelp() { return httpClient.tuiOpenHelp(); }
    public SimpleResponse tuiOpenSessions() { return httpClient.tuiOpenSessions(); }
    public SimpleResponse tuiOpenThemes() { return httpClient.tuiOpenThemes(); }
    public SimpleResponse tuiOpenModels() { return httpClient.tuiOpenModels(); }
    public SimpleResponse tuiSubmitPrompt(String prompt) { return httpClient.tuiSubmitPrompt(prompt); }
    public SimpleResponse tuiClearPrompt() { return httpClient.tuiClearPrompt(); }
    public SimpleResponse tuiExecuteCommand(String command) { return httpClient.tuiExecuteCommand(command); }
    public SimpleResponse tuiShowToast(String message, String type) { return httpClient.tuiShowToast(message, type); }
    public SimpleResponse tuiPublish(String event, Map<String, Object> data) { return httpClient.tuiPublish(event, data); }
    public SimpleResponse tuiSelectSession(String sessionId) { return httpClient.tuiSelectSession(sessionId); }

    // ============================================================
    // Instance
    // ============================================================

    public SimpleResponse disposeInstance() { return httpClient.disposeInstance(); }

    // ============================================================
    // V2 API
    // ============================================================

    public HealthStatus healthV2() { return httpClient.healthV2(); }
    public List<Agent> listAgentsV2() { return httpClient.listAgentsV2(); }
    public ApiResponse<List<Session>> listSessionsApi(Map<String, Object> params) { return httpClient.listSessionsApi(params); }
    public ApiResponse<List<ModelInfo>> listModelsApi() { return httpClient.listModelsApi(); }
    public List<Provider> listProvidersApi() { return httpClient.listProvidersApi(); }
    public Provider getProviderApi(String providerId) { return httpClient.getProviderApi(providerId); }

    // ============================================================
    // SSE 事件流
    // ============================================================

    /** V1 事件流（/event） */
    public OpenCodeSseClient sse() { return sseClient; }

    public void subscribeEvents(Consumer<Event> consumer) { sseClient.subscribe(consumer); }
    public BlockingQueue<Event> subscribeEventQueue() { return sseClient.subscribeQueue(); }

    /** 全局事件流（/global/event） */
    public void subscribeGlobalEvents(Consumer<Event> consumer) { sseClient.subscribeGlobal(consumer); }
    public BlockingQueue<Event> subscribeGlobalEventQueue() { return sseClient.subscribeGlobalQueue(); }

    /** V2 事件流（/api/event） */
    public void subscribeV2Events(Consumer<Event> consumer) { sseClient.subscribeV2(consumer); }
    public BlockingQueue<Event> subscribeV2EventQueue() { return sseClient.subscribeV2Queue(); }

    /** 类型化事件流 */
    public void subscribeTypedEvents(Consumer<TypedEvent> consumer) { sseClient.subscribeTyped(consumer); }
    public void subscribeTypedEvents(OpenCodeSseClient.TypedEventHandler handler) { sseClient.subscribeTyped(handler); }

    // ============================================================
    // CLI
    // ============================================================

    public OpenCodeCli cli() { return cli; }

    // ============================================================
    // Config
    // ============================================================

    public OpenCodeClientConfig getConfig() { return config; }

    // ============================================================
    // 生命周期
    // ============================================================

    @Override
    public void close() {
        httpClient.close();
        sseClient.close();
    }
}
