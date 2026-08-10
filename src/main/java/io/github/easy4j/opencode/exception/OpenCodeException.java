package io.github.easy4j.opencode.exception;

/**
 * Base exception for the OpenCode SDK.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see OpenCodeHttpException
 * @see OpenCodeCliStartupException
 */
public class OpenCodeException extends RuntimeException {

    /**
     * 创建 open code exception 实例，并按传入依赖确定资源所有权。
     *
     * @param message 传递给 OpenCode CLI 的提示文本
     */
    public OpenCodeException(String message) {
        super(message);
    }

    /**
     * 创建 open code exception 实例，并按传入依赖确定资源所有权。
     *
     * @param message 传递给 OpenCode CLI 的提示文本
     * @param cause 导致当前异常的原始原因
     */
    public OpenCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
