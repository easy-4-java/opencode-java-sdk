package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * 权限请求（GET /permission）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PermissionRequest {

    private String id;

    @JsonProperty("sessionID")
    private String sessionID;

    private String tool;

    @JsonProperty("toolCallID")
    private String toolCallID;

    private Map<String, Object> args;

    private String message;

    @JsonProperty("createdAt")
    private String createdAt;
}
