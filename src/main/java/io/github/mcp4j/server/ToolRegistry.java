package io.github.mcp4j.server;

import io.github.mcp4j.annotation.McpTool;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Scans objects for {@link McpTool}-annotated methods and maintains a registry of tools.
 */
public class ToolRegistry {

    private final Map<String, ToolDefinition> tools = new LinkedHashMap<>();

    public void register(Object toolInstance) {
        for (Method method : toolInstance.getClass().getMethods()) {
            if (method.isAnnotationPresent(McpTool.class)) {
                ToolDefinition def = new ToolDefinition(method, toolInstance);
                tools.put(def.getName(), def);
            }
        }
    }

    public ToolDefinition get(String name) {
        return tools.get(name);
    }

    public Collection<ToolDefinition> all() {
        return tools.values();
    }

    public boolean isEmpty() {
        return tools.isEmpty();
    }
}
