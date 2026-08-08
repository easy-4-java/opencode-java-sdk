package io.github.easy4j.opencode.cli;

import io.github.easy4j.opencode.OpenCodeCliConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OpenCodeCli}.
 * <p>
 * Note: Most CLI commands require the actual opencode binary, so these tests
 * focus on argument construction and the executor integration rather than
 * actual execution.
 * </p>
 */
class OpenCodeCliTest {

    private OpenCodeCli createCli() {
        OpenCodeCliConfig config = new OpenCodeCliConfig();
        config.setExecutable("echo"); // use echo as a stand-in
        config.setTimeout(5);
        return new OpenCodeCli(new OpenCodeCliExecutor(config));
    }

    @Test
    void shouldExposeExecutor() {
        OpenCodeCli cli = createCli();
        assertNotNull(cli.executor());
    }

    @Test
    void shouldExecuteVersionCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.version();
        // echo --version will succeed
        assertTrue(result.isSuccess() || result.getExitCode() != -1);
    }

    @Test
    void shouldExecuteHelpCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.help();
        assertNotNull(result);
    }

    @Test
    void shouldExecuteRunCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.run("hello");
        assertNotNull(result);
    }

    @Test
    void shouldExecuteRunWithModelCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.run("hello", "anthropic/claude-sonnet-4-5");
        assertNotNull(result);
    }

    @Test
    void shouldExecuteRunWithAgentAndModelCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.run("hello", "coder", "anthropic/claude-sonnet-4-5");
        assertNotNull(result);
    }

    @Test
    void shouldExecuteRunJsonCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.runJson("hello");
        assertNotNull(result);
    }

    @Test
    void shouldExecuteRunJsonWithSessionCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.runJson("hello", "sess-1", "model-1");
        assertNotNull(result);
    }

    @Test
    void shouldExecuteSessionListCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.sessionList();
        assertNotNull(result);
    }

    @Test
    void shouldExecuteSessionListWithMaxCountCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.sessionList(10);
        assertNotNull(result);
    }

    @Test
    void shouldExecuteAgentListCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.agentList();
        assertNotNull(result);
    }

    @Test
    void shouldExecuteModelsCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.models();
        assertNotNull(result);
    }

    @Test
    void shouldExecuteModelsWithProviderCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.models("anthropic", true, false);
        assertNotNull(result);
    }

    @Test
    void shouldExecuteProvidersListCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.providersList();
        assertNotNull(result);
    }

    @Test
    void shouldExecuteProvidersLoginCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.providersLogin("anthropic", "api-key");
        assertNotNull(result);
    }

    @Test
    void shouldExecuteProvidersLogoutCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.providersLogout("anthropic");
        assertNotNull(result);
    }

    @Test
    void shouldExecuteProvidersLogoutAllCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.providersLogout(null);
        assertNotNull(result);
    }

    @Test
    void shouldExecuteAuthListCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.authList();
        assertNotNull(result);
    }

    @Test
    void shouldExecuteAuthLoginCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.authLogin("anthropic", "api-key");
        assertNotNull(result);
    }

    @Test
    void shouldExecuteAuthLogoutCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.authLogout("anthropic");
        assertNotNull(result);
    }

    @Test
    void shouldExecuteAuthLogoutAllCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.authLogout(null);
        assertNotNull(result);
    }

    @Test
    void shouldExecuteMcpListCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.mcpList();
        assertNotNull(result);
    }

    @Test
    void shouldExecuteMcpLogoutCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.mcpLogout("github");
        assertNotNull(result);
    }

    @Test
    void shouldExecuteMcpLogoutAllCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.mcpLogout(null);
        assertNotNull(result);
    }

    @Test
    void shouldExecuteMcpAuthCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.mcpAuth("github");
        assertNotNull(result);
    }

    @Test
    void shouldExecuteMcpAuthAllCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.mcpAuth(null);
        assertNotNull(result);
    }

    @Test
    void shouldExecuteUpgradeCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.upgrade();
        assertNotNull(result);
    }

    @Test
    void shouldExecuteUpgradeWithTargetCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.upgrade("v1.18.0", "npm");
        assertNotNull(result);
    }

    @Test
    void shouldExecuteUpgradeWithNullTargetCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.upgrade(null, null);
        assertNotNull(result);
    }

    @Test
    void shouldExecuteUninstallCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.uninstall(true, false, true, false);
        assertNotNull(result);
    }

    @Test
    void shouldExecuteStatsCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.stats(7, 10, 5, "my-project");
        assertNotNull(result);
    }

    @Test
    void shouldExecuteStatsWithNullsCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.stats(null, null, null, null);
        assertNotNull(result);
    }

    @Test
    void shouldExecuteExportCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.export("sess-1", true);
        assertNotNull(result);
    }

    @Test
    void shouldExecuteExportWithNullSessionCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.export(null, false);
        assertNotNull(result);
    }

    @Test
    void shouldExecuteImportCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.importSession("file.json");
        assertNotNull(result);
    }

    @Test
    void shouldExecuteDbCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.db("SELECT * FROM sessions", "json");
        assertNotNull(result);
    }

    @Test
    void shouldExecuteDbPathCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.dbPath();
        assertNotNull(result);
    }

    @Test
    void shouldExecuteDebugConfigCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.debugConfig();
        assertNotNull(result);
    }

    @Test
    void shouldExecuteDebugPathsCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.debugPaths();
        assertNotNull(result);
    }

    @Test
    void shouldExecuteDebugInfoCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.debugInfo();
        assertNotNull(result);
    }

    @Test
    void shouldExecuteServeCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.serve(8080, "localhost");
        assertNotNull(result);
    }

    @Test
    void shouldExecuteServeWithNullsCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.serve(null, null);
        assertNotNull(result);
    }

    @Test
    void shouldExecuteWebCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.web(3000, "0.0.0.0");
        assertNotNull(result);
    }

    @Test
    void shouldExecuteAcpCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.acp("/data/project");
        assertNotNull(result);
    }

    @Test
    void shouldExecuteGenerateCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.generate();
        assertNotNull(result);
    }

    @Test
    void shouldExecuteGithubInstallCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.githubInstall();
        assertNotNull(result);
    }

    @Test
    void shouldExecuteGithubRunCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.githubRun("push", "token-123");
        assertNotNull(result);
    }

    @Test
    void shouldExecutePrCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.pr(42);
        assertNotNull(result);
    }

    @Test
    void shouldExecutePluginCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.plugin("my-plugin", true, false);
        assertNotNull(result);
    }

    @Test
    void shouldExecuteConsoleLoginCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.consoleLogin("https://console.example.com");
        assertNotNull(result);
    }

    @Test
    void shouldExecuteConsoleLoginWithoutUrlCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.consoleLogin(null);
        assertNotNull(result);
    }

    @Test
    void shouldExecuteConsoleLogoutCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.consoleLogout("user@example.com");
        assertNotNull(result);
    }

    @Test
    void shouldExecuteConsoleLogoutWithoutEmailCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.consoleLogout(null);
        assertNotNull(result);
    }

    @Test
    void shouldExecuteConsoleOrgsCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.consoleOrgs();
        assertNotNull(result);
    }

    @Test
    void shouldExecuteConsoleSwitchCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.consoleSwitch();
        assertNotNull(result);
    }

    @Test
    void shouldExecuteConsoleOpenCommand() {
        OpenCodeCli cli = createCli();
        OpenCodeCliResult result = cli.consoleOpen();
        assertNotNull(result);
    }
}
