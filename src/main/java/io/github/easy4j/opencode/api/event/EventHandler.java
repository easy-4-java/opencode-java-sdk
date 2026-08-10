package io.github.easy4j.opencode.api.event;

import io.github.easy4j.opencode.api.sse.SseEvent;

import java.util.Map;

/**
 * Typed callback handler for OpenCode SSE events.
 *
 * <p>All methods have no-op defaults; callers override only the callbacks they care about.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * client.onSessionEvent(sessionId, new EventHandler() {
 *     public void onTextDelta(String delta, SseEvent event) {
 *         System.out.print(delta);
 *     }
 * });
 * }</pre>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeSseClient
 */
public interface EventHandler {

    /**
     * 任一事件到达时优先触发（最早）。可作为兜底钩子。
     */
    default void onEvent(SseEvent event) {
    }

    /**
     * session 进入 idle 状态（agent loop 完成）时触发。
     */
    default void onSessionIdle(String sessionId, SseEvent event) {
    }

    /**
     * session 发生错误时触发。
     */
    default void onSessionError(String sessionId, String error, SseEvent event) {
    }

    /**
     * 文本增量（{@code message.part.updated} / {@code text.delta} 等）。
     *
     * @param delta 当前增量文本
     */
    default void onTextDelta(String delta, SseEvent event) {
    }

    /**
     * 工具调用（{@code tool_use} part）。
     */
    default void onToolCall(String toolName, Map<String, Object> input, SseEvent event) {
    }

    /**
     * 工具调用结果（{@code tool_result} part）。
     */
    default void onToolResult(String toolUseId, Object output, SseEvent event) {
    }

    /**
     * Message 创建事件（user / assistant）。
     */
    default void onMessage(String messageId, String role, SseEvent event) {
    }

    /**
     * Session 状态变更（idle / running / retry / compacting）。
     */
    default void onSessionStatus(String sessionId, String status, SseEvent event) {
    }

    /**
     * 文件 diff 事件。
     */
    default void onFileDiff(String path, SseEvent event) {
    }

    /**
     * 权限请求事件。
     */
    default void onPermissionRequested(String sessionId, String permissionId, SseEvent event) {
    }

    /**
     * 问题请求事件。
     */
    default void onQuestionRequested(String sessionId, String requestId, SseEvent event) {
    }
}