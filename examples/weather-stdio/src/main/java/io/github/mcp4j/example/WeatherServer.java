package io.github.mcp4j.example;

import io.github.mcp4j.McpServer;

/**
 * Runnable MCP server with weather tools, communicates over stdio.
 *
 * <p>Build: {@code mvn package}
 * <p>Run:   {@code java -jar target/example-weather-stdio-*.jar}
 *
 * <p>Claude Desktop config:
 * <pre>
 * {
 *   "mcpServers": {
 *     "weather": {
 *       "command": "java",
 *       "args": ["-jar", "/path/to/example-weather-stdio-0.2.0.jar"]
 *     }
 *   }
 * }
 * </pre>
 */
public class WeatherServer {

    public static void main(String[] args) {
        WeatherTools weatherTools = new WeatherTools();

        McpServer.builder()
                .serverInfo("weather-server", "1.0.0")
                .tools(weatherTools)
                .resources(weatherTools)
                .build()
                .startStdio();
    }
}
