package io.github.hiwepy.opencode.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * SSE 事件基类，包含所有事件共享的字段。
 * <p>
 * OpenCode 的 SSE 事件名恒为 {@code "message"}，数据为 JSON 序列化的 EventV2。
 * 事件类型由 {@link #type} 字段标识，格式为 {@code session.next.<event>}。
 * </p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TypedEvent {

    /** 事件类型，如 {@code session.next.text.delta} */
    private String type;

    /** 事件属性（payload），子类可直接访问 */
    protected Map<String, Object> properties;

    // -- 常用快捷访问 --

    @JsonProperty("sessionID")
    public String getSessionID() {
        return prop("sessionID");
    }

    @JsonProperty("messageID")
    public String getMessageID() {
        return prop("messageID");
    }

    @JsonProperty("runID")
    public String getRunID() {
        return prop("runID");
    }

    protected String prop(String key) {
        if (properties == null) return null;
        Object v = properties.get(key);
        return v != null ? v.toString() : null;
    }

    protected boolean propBool(String key) {
        if (properties == null) return false;
        Object v = properties.get(key);
        return v instanceof Boolean ? (Boolean) v : false;
    }
}
