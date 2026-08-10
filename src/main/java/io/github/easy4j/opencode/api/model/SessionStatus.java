package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Represents the status of a single session, returned in {@code GET /session/status}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#getSessionStatusMap()
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionStatus {

    /**
     * "idle" / "running" / "retry" / "compacting"
     */
    private String type;

    /**
     * 状态附带的诊断说明；正常状态下可能为空。
     */
    private String message;
}
