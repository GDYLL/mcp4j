package io.github.mcp4j.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an MCP resource provider.
 *
 * <p>Resources allow AI models to read content (files, database rows, API responses, etc.)
 * from your server. The annotated method must return a {@link String}.
 *
 * <pre>{@code
 * @McpResource(
 *     uri      = "file:///logs/app.log",
 *     name     = "Application Log",
 *     mimeType = "text/plain"
 * )
 * public String getAppLog() {
 *     return Files.readString(Path.of("/logs/app.log"));
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpResource {

    /**
     * Unique URI identifying this resource (e.g. {@code file:///data/config.json}).
     */
    String uri();

    /**
     * Human-readable name shown to the AI model.
     */
    String name();

    /**
     * Optional description of the resource content.
     */
    String description() default "";

    /**
     * MIME type of the returned content. Defaults to {@code text/plain}.
     */
    String mimeType() default "text/plain";
}
