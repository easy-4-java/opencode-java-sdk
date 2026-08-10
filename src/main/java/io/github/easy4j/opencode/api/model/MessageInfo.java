package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Detailed information for a single message, returned by {@code GET /session/:id/message/:messageID}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#getMessage(String, String)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageInfo {

    /**
     * OpenCode 协议字段 {@code info}；Java 类型为 {@code Message}。
     */
    private Message info;

    /**
     * OpenCode 协议字段 {@code parts} 的集合值；为空表示服务端未返回对应条目。
     */
    private List<Part> parts;
}
