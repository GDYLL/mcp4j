package io.github.mcp4j.example;

import io.github.mcp4j.McpServer;
import io.github.mcp4j.transport.SseTransport;

/**
 * Runnable MCP server with database tools, communicates over HTTP/SSE.
 *
 * <p>Build: {@code mvn package}
 * <p>Run:   {@code java -jar target/example-database-sse-*.jar}
 * <p>The server listens on {@code http://localhost:8765/sse}
 */
public class DatabaseServer {

    private static final int PORT = 8765;

    public static void main(String[] args) {
        DatabaseTools dbTools = new DatabaseTools();

        McpServer.builder()
                .serverInfo("database-server", "1.0.0")
                .tools(dbTools)
                .resources(dbTools)
                .build()
                .start(new SseTransport(PORT));
    }
}
