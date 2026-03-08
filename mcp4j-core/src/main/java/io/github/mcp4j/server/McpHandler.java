package io.github.mcp4j.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.mcp4j.protocol.JsonRpcError;
import io.github.mcp4j.protocol.JsonRpcRequest;
import io.github.mcp4j.protocol.JsonRpcResponse;
import io.github.mcp4j.resource.ResourceDefinition;
import io.github.mcp4j.resource.ResourceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core MCP protocol handler. Processes JSON-RPC messages and dispatches to tools and resources.
 */
public class McpHandler {

    private static final Logger log = LoggerFactory.getLogger(McpHandler.class);
    private static final String PROTOCOL_VERSION = "2024-11-05";

    private final ToolRegistry toolRegistry;
    private final ResourceRegistry resourceRegistry;
    private final ObjectMapper mapper;
    private final String serverName;
    private final String serverVersion;

    public McpHandler(ToolRegistry toolRegistry, ResourceRegistry resourceRegistry,
                      String serverName, String serverVersion) {
        this.toolRegistry = toolRegistry;
        this.resourceRegistry = resourceRegistry;
        this.serverName = serverName;
        this.serverVersion = serverVersion;
        this.mapper = new ObjectMapper();
    }

    /**
     * Processes a single JSON-RPC message string and returns the response string,
     * or null if the message is a notification (no response needed).
     */
    public String handle(String json) {
        JsonRpcRequest request;
        try {
            request = mapper.readValue(json, JsonRpcRequest.class);
        } catch (Exception e) {
            return toJson(JsonRpcResponse.error(null, JsonRpcError.parseError(e.getMessage())));
        }

        if (request.isNotification()) {
            log.debug("Received notification: {}", request.method());
            return null;
        }

        JsonNode id = request.id();
        try {
            JsonNode result = switch (request.method()) {
                case "initialize"       -> handleInitialize();
                case "tools/list"       -> handleToolsList();
                case "tools/call"       -> handleToolsCall(request);
                case "resources/list"   -> handleResourcesList();
                case "resources/read"   -> handleResourcesRead(request);
                case "ping"             -> mapper.createObjectNode();
                default -> throw new McpException(JsonRpcError.methodNotFound(request.method()));
            };
            return toJson(JsonRpcResponse.success(id, result));
        } catch (McpException e) {
            return toJson(JsonRpcResponse.error(id, e.getError()));
        } catch (Exception e) {
            log.error("Internal error handling method: {}", request.method(), e);
            return toJson(JsonRpcResponse.error(id, JsonRpcError.internalError(e.getMessage())));
        }
    }

    private JsonNode handleInitialize() {
        ObjectNode result = mapper.createObjectNode();
        result.put("protocolVersion", PROTOCOL_VERSION);

        ObjectNode capabilities = result.putObject("capabilities");
        if (!toolRegistry.isEmpty()) {
            capabilities.putObject("tools");
        }
        if (!resourceRegistry.isEmpty()) {
            capabilities.putObject("resources");
        }

        ObjectNode info = result.putObject("serverInfo");
        info.put("name", serverName);
        info.put("version", serverVersion);

        return result;
    }

    private JsonNode handleToolsList() {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode tools = result.putArray("tools");

        for (ToolDefinition def : toolRegistry.all()) {
            ObjectNode tool = tools.addObject();
            tool.put("name", def.getName());
            tool.put("description", def.getDescription());
            tool.putObject("inputSchema").setAll(def.toSchema(mapper));
        }

        return result;
    }

    private JsonNode handleToolsCall(JsonRpcRequest request) throws McpException {
        JsonNode params = request.params();
        if (params == null || !params.has("name")) {
            throw new McpException(JsonRpcError.invalidParams("Missing 'name' field"));
        }

        String toolName = params.get("name").asText();
        ToolDefinition def = toolRegistry.get(toolName);
        if (def == null) {
            throw new McpException(JsonRpcError.invalidParams("Unknown tool: " + toolName));
        }

        JsonNode arguments = params.has("arguments") ? params.get("arguments") : null;
        Object returnValue;
        try {
            returnValue = def.invoke(arguments, mapper);
        } catch (Exception e) {
            log.error("Tool '{}' threw an exception", toolName, e);
            throw new McpException(JsonRpcError.internalError(e.getMessage()));
        }

        ObjectNode result = mapper.createObjectNode();
        ArrayNode content = result.putArray("content");
        ObjectNode textContent = content.addObject();
        textContent.put("type", "text");
        textContent.put("text", returnValue != null ? returnValue.toString() : "");

        return result;
    }

    private JsonNode handleResourcesList() {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode resources = result.putArray("resources");

        for (ResourceDefinition def : resourceRegistry.all()) {
            ObjectNode resource = resources.addObject();
            resource.put("uri", def.getUri());
            resource.put("name", def.getName());
            if (!def.getDescription().isBlank()) {
                resource.put("description", def.getDescription());
            }
            resource.put("mimeType", def.getMimeType());
        }

        return result;
    }

    private JsonNode handleResourcesRead(JsonRpcRequest request) throws McpException {
        JsonNode params = request.params();
        if (params == null || !params.has("uri")) {
            throw new McpException(JsonRpcError.invalidParams("Missing 'uri' field"));
        }

        String uri = params.get("uri").asText();
        ResourceDefinition def = resourceRegistry.get(uri);
        if (def == null) {
            throw new McpException(JsonRpcError.invalidParams("Unknown resource: " + uri));
        }

        String content;
        try {
            content = def.read();
        } catch (Exception e) {
            log.error("Resource '{}' read failed", uri, e);
            throw new McpException(JsonRpcError.internalError(e.getMessage()));
        }

        ObjectNode result = mapper.createObjectNode();
        ArrayNode contents = result.putArray("contents");
        ObjectNode item = contents.addObject();
        item.put("uri", uri);
        item.put("mimeType", def.getMimeType());
        item.put("text", content);

        return result;
    }

    private String toJson(JsonRpcResponse response) {
        try {
            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            return "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"Serialization error\"}}";
        }
    }
}
