package io.github.hiwepy.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * OpenCode 已注册命令，对应 {@code GET /command} 响应元素。
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