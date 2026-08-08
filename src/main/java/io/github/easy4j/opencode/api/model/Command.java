package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Represents a registered slash command in OpenCode, corresponding to a {@code GET /command} response element.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#listCommands()
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Command {

    private String name;

    private String description;

    /** 模板（slash command 模板串） */
    private String template;

    /** 模板参数列表 */
    private List<String> args;

    /** agent 名称（限定该命令在哪个 agent 下生效） */
    private String agent;
}