package io.github.mcp4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mcp4j.annotation.McpParam;
import io.github.mcp4j.annotation.McpTool;
import io.github.mcp4j.server.McpHandler;
import io.github.mcp4j.server.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class McpHandlerTest {

    private McpHandler handler;
    private ObjectMapper mapper;

    static class SampleTools {
        @McpTool(description = "Returns a greeting")
        public String greet(@McpParam(value = "name", description = "Person's name") String name) {
            return "Hello, " + name + "!";
        }

        @McpTool(name = "add_numbers", description = "Adds two integers")
        public int add(
                @McpParam("a") int a,
                @McpParam("b") int b) {
            return a + b;
        }
    }

    @BeforeEach
    void setUp() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new SampleTools());
        handler = new McpHandler(registry, new io.github.mcp4j.resource.ResourceRegistry(), "test-server", "1.0.0");
        mapper = new ObjectMapper();
    }

    @Test
    void initialize_returnsServerInfo() throws Exception {
        String response = handler.handle("""
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}
                """);

        JsonNode node = mapper.readTree(response);
        assertEquals("2.0", node.get("jsonrpc").asText());
        assertEquals("test-server", node.at("/result/serverInfo/name").asText());
        assertEquals("1.0.0", node.at("/result/serverInfo/version").asText());
        assertFalse(node.has("error"));
    }

    @Test
    void toolsList_returnsRegisteredTools() throws Exception {
        String response = handler.handle("""
                {"jsonrpc":"2.0","id":2,"method":"tools/list"}
                """);

        JsonNode node = mapper.readTree(response);
        JsonNode tools = node.at("/result/tools");
        assertTrue(tools.isArray());
        assertEquals(2, tools.size());

        // Check tool names
        boolean hasGreet = false, hasAdd = false;
        for (JsonNode tool : tools) {
            String name = tool.get("name").asText();
            if ("greet".equals(name)) hasGreet = true;
            if ("add_numbers".equals(name)) hasAdd = true;
        }
        assertTrue(hasGreet, "Expected 'greet' tool");
        assertTrue(hasAdd, "Expected 'add_numbers' tool");
    }

    @Test
    void toolsCall_greet_returnsCorrectText() throws Exception {
        String response = handler.handle("""
                {"jsonrpc":"2.0","id":3,"method":"tools/call",
                 "params":{"name":"greet","arguments":{"name":"World"}}}
                """);

        JsonNode node = mapper.readTree(response);
        String text = node.at("/result/content/0/text").asText();
        assertEquals("Hello, World!", text);
    }

    @Test
    void toolsCall_addNumbers_returnsSum() throws Exception {
        String response = handler.handle("""
                {"jsonrpc":"2.0","id":4,"method":"tools/call",
                 "params":{"name":"add_numbers","arguments":{"a":3,"b":7}}}
                """);

        JsonNode node = mapper.readTree(response);
        String text = node.at("/result/content/0/text").asText();
        assertEquals("10", text);
    }

    @Test
    void unknownMethod_returnsMethodNotFoundError() throws Exception {
        String response = handler.handle("""
                {"jsonrpc":"2.0","id":5,"method":"unknown/method"}
                """);

        JsonNode node = mapper.readTree(response);
        assertEquals(-32601, node.at("/error/code").asInt());
    }

    @Test
    void notification_returnsNull() {
        // Notifications have no "id" field — no response expected
        String response = handler.handle("""
                {"jsonrpc":"2.0","method":"notifications/initialized"}
                """);
        assertNull(response);
    }

    @Test
    void invalidJson_returnsParseError() throws Exception {
        String response = handler.handle("not valid json");
        JsonNode node = mapper.readTree(response);
        assertEquals(-32700, node.at("/error/code").asInt());
    }

    @Test
    void toolsCall_unknownTool_returnsInvalidParams() throws Exception {
        String response = handler.handle("""
                {"jsonrpc":"2.0","id":6,"method":"tools/call",
                 "params":{"name":"nonexistent","arguments":{}}}
                """);

        JsonNode node = mapper.readTree(response);
        assertEquals(-32602, node.at("/error/code").asInt());
    }
}
