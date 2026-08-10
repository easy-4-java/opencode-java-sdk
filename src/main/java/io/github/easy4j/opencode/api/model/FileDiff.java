package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents a file diff entry returned by {@code GET /session/:id/diff}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#getSessionDiff(String, String)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileDiff {

    /**
     * 相对于项目根目录的文件路径。
     */
    private String path;

    /**
     * 文件重命名或移动前的旧路径；其他差异类型为空。
     */
    @JsonProperty("oldPath")
    private String oldPath;

    /**
     * 资源当前状态，具体枚举值由 OpenCode Server 定义。
     */
    private String status;

    /**
     * 差异中新增的行数。
     */
    private Integer additions;

    /**
     * 差异中删除的行数。
     */
    private Integer deletions;

    /**
     * 统一差异格式的补丁文本。
     */
    private String patch;
}
