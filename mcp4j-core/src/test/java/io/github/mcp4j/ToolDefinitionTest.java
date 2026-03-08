package io.github.mcp4j;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.mcp4j.annotation.McpParam;
import io.github.mcp4j.annotation.McpTool;
import io.github.mcp4j.server.ToolDefinition;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class ToolDefinitionTest {

    static class MyTools {
        @McpTool(description = "Search files")
        public String searchFiles(
                @McpParam(value = "query", description = "Search term") String query,
                @McpParam(value = "limit", description = "Max results", required = false) int limit) {
            return query + ":" + limit;
        }
    }

    @Test
    void camelCase_methodName_convertedToSnakeCase() throws Exception {
        Method method = MyTools.class.getMethod("searchFiles", String.class, int.class);
        ToolDefinition def = new ToolDefinition(method, new MyTools());
        assertEquals("search_files", def.getName());
    }

    @Test
    void schema_containsRequiredAndOptionalParams() throws Exception {
        Method method = MyTools.class.getMethod("searchFiles", String.class, int.class);
        ToolDefinition def = new ToolDefinition(method, new MyTools());

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode schema = def.toSchema(mapper);

        assertTrue(schema.at("/properties/query").has("type"));
        assertTrue(schema.at("/properties/limit").has("type"));

        // only 'query' is required
        boolean queryRequired = false;
        for (var node : schema.get("required")) {
            if ("query".equals(node.asText())) queryRequired = true;
        }
        assertTrue(queryRequired);
        assertEquals(1, schema.get("required").size());
    }

    @Test
    void invoke_returnsCorrectResult() throws Exception {
        Method method = MyTools.class.getMethod("searchFiles", String.class, int.class);
        ToolDefinition def = new ToolDefinition(method, new MyTools());

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode args = mapper.createObjectNode();
        args.put("query", "hello");
        args.put("limit", 5);

        Object result = def.invoke(args, mapper);
        assertEquals("hello:5", result);
    }
}
