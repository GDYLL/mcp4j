package io.github.mcp4j;

import io.github.mcp4j.resource.ResourceRegistry;
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
 *     .tools(new WeatherTools())
 *     .resources(new FileResources())
 *     .build()
 *     .startStdio();
 * }</pre>
 */
public class McpServer {

    private final McpHandler handler;

    private McpServer(McpHandler handler) {
        this.handler = handler;
    }

    /** Starts the server using stdio transport. Blocks until stdin is closed. */
    public void startStdio() {
        new StdioTransport().start(handler);
    }

    /** Starts the server using a custom transport. */
    public void start(Transport transport) {
        transport.start(handler);
    }

    /** Exposes the handler for use by transports that need direct access (e.g. SSE). */
    public McpHandler getHandler() {
        return handler;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String serverName    = "mcp4j-server";
        private String serverVersion = "0.2.0";
        private final ToolRegistry toolRegistry      = new ToolRegistry();
        private final ResourceRegistry resourceRegistry = new ResourceRegistry();

        public Builder serverInfo(String name, String version) {
            this.serverName = name;
            this.serverVersion = version;
            return this;
        }

        /**
         * Registers tool provider objects. Methods annotated with
         * {@link io.github.mcp4j.annotation.McpTool} will be exposed as MCP tools.
         */
        public Builder tools(Object... instances) {
            for (Object instance : instances) {
                toolRegistry.register(instance);
            }
            return this;
        }

        /**
         * Registers resource provider objects. Methods annotated with
         * {@link io.github.mcp4j.annotation.McpResource} will be exposed as MCP resources.
         */
        public Builder resources(Object... instances) {
            for (Object instance : instances) {
                resourceRegistry.register(instance);
            }
            return this;
        }

        public McpServer build() {
            McpHandler handler = new McpHandler(
                    toolRegistry, resourceRegistry, serverName, serverVersion);
            return new McpServer(handler);
        }
    }
}
