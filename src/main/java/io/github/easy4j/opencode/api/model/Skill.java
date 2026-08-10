package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Represents a registered skill in OpenCode, returned by {@code GET /skill}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#listSkills()
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Skill {

    /**
     * 资源的可读名称。
     */
    private String name;

    /**
     * 资源用途或能力的可读说明。
     */
    private String description;

    /**
     * OpenCode 协议字段 {@code location}；Java 类型为 {@code String}。
     */
    private String location;

    /**
     * 按到达顺序累计的流式文本内容。
     */
    private String content;
}
