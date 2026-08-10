package io.github.easy4j.opencode.api.event;

import io.github.easy4j.opencode.api.sse.SseEvent;

import java.util.Map;

/**
 * Typed callback handler for OpenCode SSE events.
 * <p>All methods have no-op defaults; callers override only the callbacks they care about.</p>
 * <p>Usage example:</p>
 * <pre>{@code
 * client.onSessionEvent(sessionId, new EventHandler() {
 *     public void onTextDelta(String delta, SseEvent event) {
 *         System.out.print(delta);
 *     }
 * });
 * }</pre>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeSseClient
 */
public interface EventHandler {

    /**
     * 任一事件到达时优先触发（最早）。可作为兜底钩子。
     *
     * @param event 触发当前回调的完整 SSE 事件
     */
    default void onEvent(SseEvent event) {
    }

    /**
     * session 进入 idle 状态（agent loop 完成）时触发。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param event 触发当前回调的完整 SSE 事件
     */
    default void onSessionIdle(String sessionId, SseEvent event) {
    }

    /**
     * session 发生错误时触发。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param error 导致流式响应失败的异常
     * @param event 触发当前回调的完整 SSE 事件
     */
    default void onSessionError(String sessionId, String error, SseEvent event) {
    }

    /**
     * 文本增量（{@code message.part.updated} / {@code text.delta} 等）。
     *
     * @param delta 本次到达的文本增量；为空时忽略
     * @param event 触发当前回调的完整 SSE 事件
     */
    default void onTextDelta(String delta, SseEvent event) {
    }

    /**
     * 工具调用（{@code tool_use} part）。
     *
     * @param toolName 被调用的工具名称
     * @param input 工具调用输入参数映射
     * @param event 触发当前回调的完整 SSE 事件
     */
    default void onToolCall(String toolName, Map<String, Object> input, SseEvent event) {
    }

    /**
     * 工具调用结果（{@code tool_result} part）。
     *
     * @param toolUseId 关联工具调用与结果的唯一 ID
     * @param output 工具调用返回的原始结果对象
     * @param event 触发当前回调的完整 SSE 事件
     */
    default void onToolResult(String toolUseId, Object output, SseEvent event) {
    }

    /**
     * Message 创建事件（user / assistant）。
     *
     * @param messageId 会话消息 ID
     * @param role 消息角色，例如 user、assistant 或 system
     * @param event 触发当前回调的完整 SSE 事件
     */
    default void onMessage(String messageId, String role, SseEvent event) {
    }

    /**
     * Session 状态变更（idle / running / retry / compacting）。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param status 会话、工具或子系统的当前状态
     * @param event 触发当前回调的完整 SSE 事件
     */
    default void onSessionStatus(String sessionId, String status, SseEvent event) {
    }

    /**
     * 文件 diff 事件。
     *
     * @param path 文件或工作目录路径
     * @param event 触发当前回调的完整 SSE 事件
     */
    default void onFileDiff(String path, SseEvent event) {
    }

    /**
     * 处理 permission requested 事件；默认实现不执行操作。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param permissionId 待处理的权限请求 ID
     * @param event 触发当前回调的完整 SSE 事件
     */
    default void onPermissionRequested(String sessionId, String permissionId, SseEvent event) {
    }

    /**
     * 处理 question requested 事件；默认实现不执行操作。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param requestId 待回复的问题或权限请求 ID
     * @param event 触发当前回调的完整 SSE 事件
     */
    default void onQuestionRequested(String sessionId, String requestId, SseEvent event) {
    }
}
