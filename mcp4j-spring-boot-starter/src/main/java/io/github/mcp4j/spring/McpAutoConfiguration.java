package io.github.mcp4j.spring;

import io.github.mcp4j.McpServer;
import io.github.mcp4j.annotation.McpResource;
import io.github.mcp4j.annotation.McpTool;
import io.github.mcp4j.transport.SseTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Auto-configures an {@link McpServer} by scanning the Spring context for beans
 * that contain {@link McpTool} or {@link McpResource} annotated methods.
 *
 * <p>Just add the starter dependency and define your tool/resource beans — no boilerplate needed.
 *
 * <pre>{@code
 * @Component
 * public class WeatherTools {
 *
 *     @McpTool(description = "Get weather for a city")
 *     public String getWeather(@McpParam("city") String city) {
 *         return "Sunny in " + city;
 *     }
 * }
 * }</pre>
 */
@AutoConfiguration
@EnableConfigurationProperties(McpProperties.class)
public class McpAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(McpAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public McpServer mcpServer(ApplicationContext context, McpProperties props) {
        McpServer.Builder builder = McpServer.builder()
                .serverInfo(props.getServerName(), props.getServerVersion());

        List<Object> toolProviders     = new ArrayList<>();
        List<Object> resourceProviders = new ArrayList<>();

        for (Map.Entry<String, Object> entry : context.getBeansOfType(Object.class).entrySet()) {
            Object bean = entry.getValue();
            Class<?> cls = bean.getClass();
            boolean hasTool = false, hasResource = false;
            for (Method method : cls.getMethods()) {
                if (method.isAnnotationPresent(McpTool.class))     hasTool = true;
                if (method.isAnnotationPresent(McpResource.class)) hasResource = true;
            }
            if (hasTool)     toolProviders.add(bean);
            if (hasResource) resourceProviders.add(bean);
        }

        if (!toolProviders.isEmpty()) {
            log.info("mcp4j: registered {} tool provider bean(s)", toolProviders.size());
            builder.tools(toolProviders.toArray());
        }
        if (!resourceProviders.isEmpty()) {
            log.info("mcp4j: registered {} resource provider bean(s)", resourceProviders.size());
            builder.resources(resourceProviders.toArray());
        }

        return builder.build();
    }

    @Bean
    public ApplicationRunner mcpServerRunner(McpServer mcpServer, McpProperties props) {
        return args -> {
            if (props.getTransport() == McpProperties.Transport.SSE) {
                log.info("mcp4j: starting SSE transport on port {}", props.getSsePort());
                mcpServer.start(new SseTransport(props.getSsePort()));
            } else {
                log.info("mcp4j: starting stdio transport");
                mcpServer.startStdio();
            }
        };
    }
}
