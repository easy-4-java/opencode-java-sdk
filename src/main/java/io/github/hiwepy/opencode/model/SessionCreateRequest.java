package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 创建 Session 的请求体（POST /session）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionCreateRequest {

    private String title;

    @JsonProperty("parentID")
    private String parentID;

    private String agent;

    private ModelRef model;

    private Map<String, Object> metadata;

    @JsonProperty("workspaceID")
    private String workspaceID;

    private Object permission;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ModelRef {
        private String id;

        @JsonProperty("providerID")
        private String providerID;

        private String variant;
    }
}
