package io.github.mcp4j;

import io.github.mcp4j.server.McpHandler;
import io.github.mcp4j.server.ToolRegistry;
import io.github.mcp4j.transport.StdioTransport;
import io.github.mcp4j.transport.Transport;

/**
 * Entry point for building and running an MCP server.
 *
 * <pre>{@code
 * McpServer.builder()
 *     .serverInfo("my-server", "1.0.0")
 *     .tools(new WeatherTools(), new FileTools())
 *     .build()
 *     .startStdio();
 * }</pre>
 */
public class McpServer {

    private final McpHandler handler;

    private McpServer(McpHandler handler) {
        this.handler = handler;
    }

    /**
     * Starts the server using stdio transport (for Claude Desktop and local use).
     * Blocks until stdin is closed.
     */
    public void startStdio() {
        new StdioTransport().start(handler);
    }

    /**
     * Starts the server using a custom transport.
     */
    public void start(Transport transport) {
        transport.start(handler);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String serverName    = "mcp4j-server";
        private String serverVersion = "0.1.0";
        private final ToolRegistry registry = new ToolRegistry();

        public Builder serverInfo(String name, String version) {
            this.serverName = name;
            this.serverVersion = version;
            return this;
        }

        /**
         * Registers one or more tool provider objects.
         * Any method annotated with {@link io.github.mcp4j.annotation.McpTool} will be exposed.
         */
        public Builder tools(Object... toolInstances) {
            for (Object instance : toolInstances) {
                registry.register(instance);
            }
            return this;
        }

        public McpServer build() {
            McpHandler handler = new McpHandler(registry, serverName, serverVersion);
            return new McpServer(handler);
        }
    }
}
