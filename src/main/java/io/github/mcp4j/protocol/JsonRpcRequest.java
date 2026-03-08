package io.github.mcp4j.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * JSON-RPC 2.0 request message.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JsonRpcRequest(
        String jsonrpc,
        JsonNode id,
        String method,
        JsonNode params
) {
    public boolean isNotification() {
        return id == null;
    }
}
