package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 消息 diff 信息（GET /session/:id/diff）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageDiff {

    @JsonProperty("messageID")
    private String messageID;

    private String diff;

    @JsonProperty("addedLines")
    private int addedLines;

    @JsonProperty("removedLines")
    private int removedLines;

    @JsonProperty("filesChanged")
    private List<String> filesChanged;
}
