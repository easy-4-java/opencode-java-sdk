package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Represents a registered skill in OpenCode, returned by {@code GET /skill}.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#listSkills()
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Skill {

    private String name;

    private String description;

    private String location;

    private String content;
}