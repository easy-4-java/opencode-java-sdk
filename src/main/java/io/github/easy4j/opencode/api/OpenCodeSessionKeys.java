package io.github.easy4j.opencode.api;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * OpenCode 会话 key 命名工具，对齐 {@code OpenClawSessionKeys} 的设计。
 *
 * <p>OpenCode Server 基于 session 模型，{@code sessionId} 由服务端生成（{@code ses} 前缀），
 * 无法由客户端指定。本工具用 {@code sessionKey} 作为 session 的 {@code title}，
 * 配合 {@code OpenCodeClient#prompt(String sessionKey, PromptRequest request)} 实现
 * 「按 sessionKey 自动复用/创建 session」的透明调用。</p>
 *
 * <ul>
 *     <li><b>固定多轮会话</b>：{@code opencode:{agentId}:{peerId}}</li>
 *     <li><b>按渠道隔离</b>：{@code opencode:{agentId}:{peerId}:{channel}}</li>
 * </ul>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 2.7.x
 */
public final class OpenCodeSessionKeys {

    /** 允许字母/数字开头，支持大小写字母、数字、下划线、点、破折号，最长128字节 */
    private static final Pattern SAFE_SEGMENT = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$");

    private OpenCodeSessionKeys() {
    }

    /**
     * 固定多轮会话 key：{@code opencode:{agentId}:{peerId}}。
     * <p>同一 agent + 同一用户复用同一会话上下文。</p>
     *
     * @param agentId 路由 agent
     * @param peerId  业务 peer（如 userId）
     */
    public static String forStableSession(String agentId, String peerId) {
        return "opencode:" + normalizeSegment(agentId, "agentId") + ":"
                + normalizeSegment(peerId, "peerId");
    }

    /**
     * 固定多轮会话 key（含渠道）：{@code opencode:{agentId}:{peerId}:{channel}}。
     * <p>同一 agent + 同一用户 + 同一渠道复用同一会话上下文。</p>
     *
     * @param agentId 路由 agent
     * @param peerId  业务 peer（如 userId）
     * @param channel 渠道（如 xiaohongshu）
     */
    public static String forStableSession(String agentId, String peerId, String channel) {
        return "opencode:" + normalizeSegment(agentId, "agentId") + ":"
                + normalizeSegment(peerId, "peerId") + ":"
                + normalizeSegment(channel, "channel");
    }

    /**
     * 规范化 session key 片段：trim、小写，并校验不含 {@code :} 与非法字符。
     */
    static String normalizeSegment(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (normalized.contains(":")) {
            throw new IllegalArgumentException(fieldName + " must not contain ':'");
        }
        if (!SAFE_SEGMENT.matcher(normalized).matches()) {
            throw new IllegalArgumentException(fieldName + " contains illegal characters: " + value);
        }
        return normalized;
    }
}
