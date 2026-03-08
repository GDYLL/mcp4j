package io.github.mcp4j.server;

import io.github.mcp4j.protocol.JsonRpcError;

/**
 * Internal exception used to carry a JSON-RPC error back to the handler.
 */
class McpException extends Exception {

    private final JsonRpcError error;

    McpException(JsonRpcError error) {
        super(error.message());
        this.error = error;
    }

    JsonRpcError getError() {
        return error;
    }
}
