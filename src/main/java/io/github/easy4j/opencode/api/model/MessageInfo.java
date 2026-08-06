package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * 单条 message 详情，对应 {@code GET /session/:id/message/:messageID}。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageInfo {

    private Message info;

    private List<Part> parts;
}