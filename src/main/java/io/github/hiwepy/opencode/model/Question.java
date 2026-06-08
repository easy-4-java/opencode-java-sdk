package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * 问题请求（GET /question）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Question {

    private String id;

    @JsonProperty("sessionID")
    private String sessionID;

    private String question;

    private Map<String, Object> options;

    @JsonProperty("createdAt")
    private String createdAt;
}
