package io.github.mcp4j.transport;

import io.github.mcp4j.server.McpHandler;

/**
 * Abstraction over the communication channel between the MCP client and server.
 */
public interface Transport {

    /**
     * Starts the transport and blocks until the connection is closed.
     *
     * @param handler the message handler to dispatch incoming requests to
     */
    void start(McpHandler handler);
}
