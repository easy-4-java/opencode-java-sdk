package io.github.easy4j.opencode.api.sse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

/** OpenCode Server 通过 SSE 传输的事件。 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SseEvent {

    private String type;
    private Map<String, Object> properties;
}
