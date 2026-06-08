package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

/**
 * 日志条目（POST /log）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogEntry {

    private String level;
    private String message;
    private String category;
    private Map<String, Object> data;
}
