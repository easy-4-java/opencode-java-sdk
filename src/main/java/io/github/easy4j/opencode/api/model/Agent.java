package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents an OpenCode agent, returned by {@code GET /agent}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#listAgents()
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Agent {

    /**
     * 资源的可读名称。
     */
    private String name;
    /**
     * 资源用途或能力的可读说明。
     */
    private String description;
    /**
     * 智能体运行模式或模式配置映射。
     */
    private String mode;

    /**
     * 默认模型标识，通常采用 provider/model 格式。
     */
    @JsonProperty("model")
    private Object model;
}
