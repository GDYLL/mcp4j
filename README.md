# mcp4j

Lightweight Java SDK for building [MCP (Model Context Protocol)](https://modelcontextprotocol.io) servers.

MCP is an open standard by Anthropic that lets AI models (like Claude) call external tools and services. **mcp4j** makes it dead simple to expose your Java code as MCP tools — just add an annotation.

## Quick Start

### 1. Add dependency

```xml
<dependency>
    <groupId>io.github.mcp4j</groupId>
    <artifactId>mcp4j</artifactId>
    <version>0.1.0</version>
</dependency>
```

### 2. Define your tools

```java
public class WeatherTools {

    @McpTool(description = "Get the current weather for a city")
    public String getWeather(
            @McpParam(value = "city", description = "City name, e.g. Beijing") String city) {
        // call your weather API here
        return "Sunny, 25°C in " + city;
    }

    @McpTool(description = "List supported cities")
    public String listCities() {
        return "Beijing, Shanghai, Guangzhou, Shenzhen";
    }
}
```

### 3. Start the server

```java
public class Main {
    public static void main(String[] args) {
        McpServer.builder()
                .serverInfo("weather-server", "1.0.0")
                .tools(new WeatherTools())
                .build()
                .startStdio();
    }
}
```

### 4. Connect to Claude Desktop

Add to `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "weather": {
      "command": "java",
      "args": ["-jar", "/path/to/your-server.jar"]
    }
  }
}
```

Claude will now be able to call your `get_weather` and `list_cities` tools.

---

## Annotations

### `@McpTool`

| Attribute     | Required | Description                                                  |
|---------------|----------|--------------------------------------------------------------|
| `description` | Yes      | Describes the tool to the AI model                           |
| `name`        | No       | Tool name (defaults to method name converted to snake_case)  |

### `@McpParam`

| Attribute     | Required | Description                                                  |
|---------------|----------|--------------------------------------------------------------|
| `value`       | No       | Parameter name (defaults to compiled parameter name)         |
| `description` | No       | Describes the parameter to the AI model                      |
| `required`    | No       | Whether the parameter is required (default: `true`)          |

---

## Supported Types

| Java type                  | JSON Schema type |
|----------------------------|------------------|
| `String`                   | `string`         |
| `int` / `Integer`          | `integer`        |
| `long` / `Long`            | `integer`        |
| `double` / `Double`        | `number`         |
| `float` / `Float`          | `number`         |
| `boolean` / `Boolean`      | `boolean`        |

---

## Transports

| Transport | Usage                        | Status |
|-----------|------------------------------|--------|
| stdio     | `.startStdio()`              | Stable |
| HTTP/SSE  | `.start(new SseTransport())` | Planned |

---

## Requirements

- Java 21+
- Maven 3.8+

## License

Apache License 2.0
