package io.github.mcp4j.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an MCP tool that can be called by AI models.
 *
 * <pre>{@code
 * @McpTool(description = "Get the current weather for a city")
 * public String getWeather(@McpParam("city") String city) {
 *     return "Sunny, 25°C in " + city;
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpTool {

    /**
     * Tool name exposed to the AI model. Defaults to the method name (camelCase -> snake_case).
     */
    String name() default "";

    /**
     * Human-readable description of what this tool does. Required.
     */
    String description();
}
