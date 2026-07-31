package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 待审批的 Permission Request，对应 {@code GET /permission}、{@code /session/:id/permission} 响应元素。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PermissionRequest {

    private String id;

    private String sessionID;

    /** 权限类型（bash / edit / webfetch 等） */
    private String permission;

    /** 描述（即将执行的命令/操作） */
    private String description;

    /** 调用参数 */
    private Map<String, Object> metadata;

    /** 已记录的相似规则 */
    private List<String> patterns;
}