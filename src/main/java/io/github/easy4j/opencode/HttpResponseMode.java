package io.github.easy4j.opencode;

/**
 * HTTP 对话响应模式。
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public enum HttpResponseMode {

    /**
     * 等待响应体完整读取后再返回，适用于普通同步 REST 调用。
     */
    BLOCKING,

    /**
     * 保持响应连接并逐段消费数据，适用于 SSE 或其他流式响应。
     */
    STREAM,

    /**
     * 由具体 API 根据端点类型自动选择缓冲或流式模式。
     */
    AUTO
}
