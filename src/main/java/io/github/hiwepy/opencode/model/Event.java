package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * SSE 事件。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {

    private String type;
    private Map<String, Object> properties;
}
