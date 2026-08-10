package io.github.easy4j.opencode.api.sse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

/**
 * OpenCode Server 通过 SSE 传输的事件。
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SseEvent {

    /**
     * OpenCode 协议中的类型判别值。
     */
    private String type;
    /**
     * OpenCode 协议字段 {@code properties} 的集合值；为空表示服务端未返回对应条目。
     */
    private Map<String, Object> properties;
}
