package io.github.hiwepy.opencode.exception;

/**
 * OpenCode SDK 异常基类。
 */
public class OpenCodeException extends RuntimeException {

    public OpenCodeException(String message) {
        super(message);
    }

    public OpenCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
