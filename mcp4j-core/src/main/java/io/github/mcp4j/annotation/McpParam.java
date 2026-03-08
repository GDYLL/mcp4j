package io.github.mcp4j.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes a parameter of an {@link McpTool} method.
 *
 * <pre>{@code
 * @McpTool(description = "Search for files")
 * public String search(
 *     @McpParam(value = "query", description = "Search keyword") String query,
 *     @McpParam(value = "limit", description = "Max results", required = false) int limit
 * ) { ... }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpParam {

    /**
     * Parameter name exposed in the JSON schema. Defaults to the compiled parameter name.
     */
    String value() default "";

    /**
     * Description of this parameter shown to the AI model.
     */
    String description() default "";

    /**
     * Whether this parameter is required. Defaults to true.
     */
    boolean required() default true;
}
