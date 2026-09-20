package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * OpenCode server configuration used by the serve and web commands.
 * Mirrors ConfigServerV1 from the supported upstream OpenCode line.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenCodeServerConfig {

    /** Port to listen on. */
    private Integer port;

    /** Hostname to listen on. */
    private String hostname;

    /** Whether mDNS service discovery is enabled. */
    private Boolean mdns;

    /** Custom mDNS domain name, for example opencode.local. */
    private String mdnsDomain;

    /** Additional CORS origins. */
    private List<String> cors;
}
