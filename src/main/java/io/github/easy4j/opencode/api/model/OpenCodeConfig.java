package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * Represents the OpenCode global or instance configuration, returned by
 * {@code GET /config} and {@code GET /global/config}.
 * <p>Fields are explicitly mapped via {@link JsonProperty} to support PATCH deserialization.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#getConfig()
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#getGlobalConfig()
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenCodeConfig {

    /**
     * OpenCode 界面主题名称。
     */
    private String theme;
    /**
     * 默认模型标识，通常采用 provider/model 格式。
     */
    private String model;
    /**
     * 默认智能体名称。
     */
    private String agent;
    /**
     * 默认模型提供方 ID。
     */
    private String provider;

    /**
     * 未显式指定时使用的默认智能体。
     */
    @JsonProperty("default_agent")
    private String defaultAgent;

    /**
     * 未显式指定时使用的默认模型。
     */
    @JsonProperty("default_model")
    private String defaultModel;

    /**
     * OpenCode Server Basic Auth 用户名。
     */
    private String username;

    /**
     * 会话共享策略，由 OpenCode Server 协议定义。
     */
    @JsonProperty("share")
    private String share;

    /**
     * 是否按旧版 autoshare 字段自动共享会话。
     */
    @JsonProperty("autoshare")
    private Boolean autoshare;

    /**
     * 是否按 auto_share 字段自动共享会话。
     */
    @JsonProperty("auto_share")
    private Boolean autoShare;

    /**
     * 智能体运行模式或模式配置映射。
     */
    private Map<String, Object> mode;

    /**
     * 兼容 provider JSON 对象的动态配置映射。
     */
    private Map<String, Object> provider_;

    /**
     * 按提供方 ID 保存的动态配置映射。
     */
    private Map<String, Object> providers;

    /**
     * 兼容 agent JSON 对象的动态配置映射。
     */
    private Map<String, Object> agent_;

    /**
     * 按智能体名称保存的动态配置映射。
     */
    private Map<String, Object> agents;

    /**
     * 权限规则配置映射。
     */
    private Map<String, Object> permission;

    /**
     * 工具启用状态及工具级配置映射。
     */
    private Map<String, Object> tools;

    /**
     * OpenCode 实验性功能配置，字段可能随服务端版本变化。
     */
    private Map<String, Object> experimental;

    /**
     * 兜底字段：未知字段落这里
     */
    private Map<String, Object> extra;
}
