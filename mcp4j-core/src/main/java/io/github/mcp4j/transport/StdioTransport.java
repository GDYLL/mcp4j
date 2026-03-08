package io.github.mcp4j.transport;

import io.github.mcp4j.server.McpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * stdio transport: reads JSON-RPC messages from stdin, writes responses to stdout.
 *
 * <p>This is the standard transport for local MCP servers (e.g. Claude Desktop).
 * stdout is reserved exclusively for protocol messages; all logging must go to stderr.
 */
public class StdioTransport implements Transport {

    private static final Logger log = LoggerFactory.getLogger(StdioTransport.class);

    @Override
    public void start(McpHandler handler) {
        // Capture stdout before redirecting, so protocol messages still go there
        PrintStream protocolOut = System.out;
        // Redirect System.out to stderr so that any accidental println calls
        // don't corrupt the protocol stream
        System.setOut(System.err);

        log.info("MCP server started on stdio");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                log.debug("--> {}", line);
                String response = handler.handle(line);
                if (response != null) {
                    log.debug("<-- {}", response);
                    protocolOut.println(response);
                    protocolOut.flush();
                }
            }
        } catch (Exception e) {
            log.error("Stdio transport error", e);
        }

        log.info("MCP server stopped");
    }
}
