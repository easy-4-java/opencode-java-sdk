package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 文件状态（Git 状态）（GET /file/status）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileStatus {

    private String path;
    private String status;  // "modified", "added", "deleted", "untracked", etc.

    @JsonProperty("isStaged")
    private boolean staged;
}
