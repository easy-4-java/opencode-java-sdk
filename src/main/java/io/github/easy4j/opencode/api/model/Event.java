package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

/**
 * SSE 事件。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {

    private String type;
    private Map<String, Object> properties;
}
