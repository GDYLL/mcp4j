package io.github.mcp4j.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.mcp4j.annotation.McpParam;
import io.github.mcp4j.annotation.McpTool;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a registered MCP tool backed by a Java method.
 */
public class ToolDefinition {

    private final String name;
    private final String description;
    private final Method method;
    private final Object instance;
    private final List<ParamInfo> params;

    public ToolDefinition(Method method, Object instance) {
        McpTool annotation = method.getAnnotation(McpTool.class);
        method.setAccessible(true);
        this.method = method;
        this.instance = instance;
        this.description = annotation.description();
        this.name = annotation.name().isBlank()
                ? camelToSnake(method.getName())
                : annotation.name();
        this.params = resolveParams(method);
    }

    public String getDescription() { return description; }

    public String getName() {
        return name;
    }

    /**
     * Builds the JSON Schema for this tool's input, used in tools/list response.
     */
    public ObjectNode toSchema(ObjectMapper mapper) {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        ArrayNode required = schema.putArray("required");

        for (ParamInfo p : params) {
            ObjectNode prop = properties.putObject(p.name());
            prop.put("type", jsonType(p.javaType()));
            if (!p.description().isBlank()) {
                prop.put("description", p.description());
            }
            if (p.required()) {
                required.add(p.name());
            }
        }

        return schema;
    }

    /**
     * Invokes the tool method with arguments from the AI model.
     */
    public Object invoke(JsonNode arguments, ObjectMapper mapper) throws Exception {
        Object[] args = new Object[params.size()];
        for (int i = 0; i < params.size(); i++) {
            ParamInfo p = params.get(i);
            JsonNode value = arguments != null ? arguments.get(p.name()) : null;
            args[i] = value != null
                    ? mapper.treeToValue(value, p.javaType())
                    : defaultValue(p.javaType());
        }
        return method.invoke(instance, args);
    }

    private List<ParamInfo> resolveParams(Method method) {
        List<ParamInfo> result = new ArrayList<>();
        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters) {
            McpParam annotation = parameter.getAnnotation(McpParam.class);
            String name = (annotation != null && !annotation.value().isBlank())
                    ? annotation.value()
                    : parameter.getName();
            String description = annotation != null ? annotation.description() : "";
            boolean required = annotation == null || annotation.required();
            result.add(new ParamInfo(name, description, required, parameter.getType()));
        }
        return result;
    }

    private String jsonType(Class<?> type) {
        if (type == String.class) return "string";
        if (type == int.class || type == Integer.class
                || type == long.class || type == Long.class) return "integer";
        if (type == double.class || type == Double.class
                || type == float.class || type == Float.class) return "number";
        if (type == boolean.class || type == Boolean.class) return "boolean";
        return "string";
    }

    private Object defaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        if (type == boolean.class) return false;
        return null;
    }

    private static String camelToSnake(String name) {
        return name.replaceAll("([A-Z])", "_$1").toLowerCase().replaceFirst("^_", "");
    }

    record ParamInfo(String name, String description, boolean required, Class<?> javaType) {}
}
