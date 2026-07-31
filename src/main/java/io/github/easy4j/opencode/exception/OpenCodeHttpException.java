package io.github.easy4j.opencode.exception;

import lombok.Getter;

/**
 * OpenCode HTTP 请求失败异常。
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
