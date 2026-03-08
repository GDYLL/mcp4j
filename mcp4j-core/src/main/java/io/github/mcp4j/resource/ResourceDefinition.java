package io.github.mcp4j.resource;

import io.github.mcp4j.annotation.McpResource;

import java.lang.reflect.Method;

/**
 * Represents a registered MCP resource backed by a Java method.
 */
public class ResourceDefinition {

    private final String uri;
    private final String name;
    private final String description;
    private final String mimeType;
    private final Method method;
    private final Object instance;

    public ResourceDefinition(Method method, Object instance) {
        McpResource annotation = method.getAnnotation(McpResource.class);
        method.setAccessible(true);
        this.uri = annotation.uri();
        this.name = annotation.name();
        this.description = annotation.description();
        this.mimeType = annotation.mimeType();
        this.method = method;
        this.instance = instance;
    }

    public String getUri() { return uri; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getMimeType() { return mimeType; }

    public String read() throws Exception {
        Object result = method.invoke(instance);
        return result != null ? result.toString() : "";
    }
}
