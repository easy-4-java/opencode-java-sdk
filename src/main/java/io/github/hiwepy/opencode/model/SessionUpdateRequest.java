package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * 更新 Session 的请求体（PATCH /session/:id）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionUpdateRequest {

    private String title;
    private String agent;
    private SessionCreateRequest.ModelRef model;
    private Map<String, Object> metadata;

    @JsonProperty("workspaceID")
    private String workspaceID;
}
