package io.github.hiwepy.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 单个 session 状态，对应 {@code GET /session/status} 响应中的 {@code {sessionID: status}}。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionStatus {

    /** "idle" / "running" / "retry" / "compacting" */
    private String type;

    private String message;
}