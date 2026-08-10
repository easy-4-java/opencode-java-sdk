package io.github.easy4j.opencode.exception;

/**
 * Base exception for the OpenCode SDK.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see OpenCodeHttpException
 * @see OpenCodeCliStartupException
 */
public class OpenCodeException extends RuntimeException {

    public OpenCodeException(String message) {
        super(message);
    }

    public OpenCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
