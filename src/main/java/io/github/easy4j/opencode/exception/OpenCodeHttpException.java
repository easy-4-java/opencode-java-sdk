package io.github.easy4j.opencode.exception;

import lombok.Getter;

/**
 * Exception thrown when an OpenCode HTTP request fails.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see OpenCodeException
 */
@Getter
public class OpenCodeHttpException extends OpenCodeException {

    private final int statusCode;
    private final String responseBody;

    public OpenCodeHttpException(int statusCode, String responseBody) {
        super("OpenCode HTTP error: " + statusCode + " - " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public OpenCodeHttpException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.responseBody = null;
    }
}
