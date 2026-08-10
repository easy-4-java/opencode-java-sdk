package io.github.easy4j.opencode.exception;

import lombok.Getter;

/**
 * Exception thrown when an OpenCode HTTP request fails.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see OpenCodeException
 */
@Getter
public class OpenCodeHttpException extends OpenCodeException {

    /**
     * HTTP 响应状态码；无响应时为负值。
     */
    private final int statusCode;
    /**
     * 服务端返回的原始响应体，可能为空。
     */
    private final String responseBody;

    /**
     * 创建 open code http exception 实例，并按传入依赖确定资源所有权。
     *
     * @param statusCode HTTP 响应状态码
     * @param responseBody 服务端返回的原始响应体，可能为空
     */
    public OpenCodeHttpException(int statusCode, String responseBody) {
        super("OpenCode HTTP error: " + statusCode + " - " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    /**
     * 创建 open code http exception 实例，并按传入依赖确定资源所有权。
     *
     * @param message 传递给 OpenCode CLI 的提示文本
     * @param cause 导致当前异常的原始原因
     */
    public OpenCodeHttpException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.responseBody = null;
    }
}
