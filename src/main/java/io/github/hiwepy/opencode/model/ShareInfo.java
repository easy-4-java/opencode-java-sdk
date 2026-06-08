package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 分享链接信息（POST/DELETE /session/:id/share）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShareInfo {

    private String url;

    @JsonProperty("shareID")
    private String shareID;

    @JsonProperty("expiresAt")
    private String expiresAt;

    @JsonProperty("createdAt")
    private String createdAt;
}
