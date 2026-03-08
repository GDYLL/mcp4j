package io.github.mcp4j.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for mcp4j.
 *
 * <pre>
 * mcp:
 *   server-name: my-server
 *   server-version: 1.0.0
 *   transport: stdio          # stdio | sse
 *   sse-port: 8765
 * </pre>
 */
@ConfigurationProperties(prefix = "mcp")
public class McpProperties {

    private String serverName    = "mcp4j-server";
    private String serverVersion = "0.2.0";
    private Transport transport  = Transport.STDIO;
    private int ssePort          = 8765;

    public enum Transport { STDIO, SSE }

    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }

    public String getServerVersion() { return serverVersion; }
    public void setServerVersion(String serverVersion) { this.serverVersion = serverVersion; }

    public Transport getTransport() { return transport; }
    public void setTransport(Transport transport) { this.transport = transport; }

    public int getSsePort() { return ssePort; }
    public void setSsePort(int ssePort) { this.ssePort = ssePort; }
}
