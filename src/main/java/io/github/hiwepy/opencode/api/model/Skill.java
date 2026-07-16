package io.github.hiwepy.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * OpenCode 已注册 Skill，对应 {@code GET /skill} 响应元素。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Skill {

    private String name;

    private String description;

    private String location;

    private String content;
}