package io.github.mcp4j.resource;

import io.github.mcp4j.annotation.McpResource;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Scans objects for {@link McpResource}-annotated methods and maintains a registry.
 */
public class ResourceRegistry {

    private final Map<String, ResourceDefinition> resources = new LinkedHashMap<>();

    public void register(Object instance) {
        for (Method method : instance.getClass().getMethods()) {
            if (method.isAnnotationPresent(McpResource.class)) {
                ResourceDefinition def = new ResourceDefinition(method, instance);
                resources.put(def.getUri(), def);
            }
        }
    }

    public ResourceDefinition get(String uri) {
        return resources.get(uri);
    }

    public Collection<ResourceDefinition> all() {
        return resources.values();
    }

    public boolean isEmpty() {
        return resources.isEmpty();
    }
}
