package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 模型信息（GET /api/model, opencode models CLI）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelInfo {

    @JsonProperty("modelID")
    private String modelID;

    @JsonProperty("providerID")
    private String providerID;

    private String name;
    private String description;

    @JsonProperty("maxTokens")
    private Integer maxTokens;

    @JsonProperty("supportsVision")
    private boolean supportsVision;

    @JsonProperty("supportsTools")
    private boolean supportsTools;

    @JsonProperty("supportsReasoning")
    private boolean supportsReasoning;

    private List<String> variants;
}
