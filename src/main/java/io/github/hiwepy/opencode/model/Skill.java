package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

/**
 * 可用技能（GET /skill）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Skill {

    private String name;
    private String description;
    private String location;

    private Map<String, Object> metadata;
}
