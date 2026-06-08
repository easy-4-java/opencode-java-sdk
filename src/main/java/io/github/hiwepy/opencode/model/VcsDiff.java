package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * VCS diff 结果（GET /vcs/diff）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VcsDiff {

    private String diff;

    private String raw;
}
