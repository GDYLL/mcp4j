package io.github.mcp4j.example;

import io.github.mcp4j.annotation.McpParam;
import io.github.mcp4j.annotation.McpResource;
import io.github.mcp4j.annotation.McpTool;

import java.util.Map;

/**
 * Example tool and resource provider for weather data.
 */
public class WeatherTools {

    private static final Map<String, String> WEATHER_DB = Map.of(
            "beijing",   "Sunny, 12°C, humidity 30%",
            "shanghai",  "Cloudy, 18°C, humidity 65%",
            "guangzhou", "Rainy, 22°C, humidity 85%",
            "shenzhen",  "Sunny, 25°C, humidity 55%",
            "chengdu",   "Foggy, 10°C, humidity 75%"
    );

    @McpTool(description = "Get the current weather for a city")
    public String getWeather(
            @McpParam(value = "city", description = "City name in lowercase, e.g. beijing") String city) {
        String result = WEATHER_DB.get(city.toLowerCase());
        return result != null ? result : "Weather data unavailable for: " + city;
    }

    @McpTool(description = "List all cities with available weather data")
    public String listCities() {
        return String.join(", ", WEATHER_DB.keySet().stream().sorted().toList());
    }

    @McpTool(name = "compare_weather", description = "Compare weather between two cities")
    public String compareWeather(
            @McpParam(value = "city1", description = "First city") String city1,
            @McpParam(value = "city2", description = "Second city") String city2) {
        String w1 = WEATHER_DB.getOrDefault(city1.toLowerCase(), "unknown");
        String w2 = WEATHER_DB.getOrDefault(city2.toLowerCase(), "unknown");
        return "%s: %s\n%s: %s".formatted(city1, w1, city2, w2);
    }

    @McpResource(
            uri         = "weather://forecast/today",
            name        = "Today's Forecast Summary",
            description = "A summary of today's weather across all tracked cities",
            mimeType    = "text/plain"
    )
    public String getTodayForecast() {
        StringBuilder sb = new StringBuilder("=== Today's Weather Forecast ===\n");
        WEATHER_DB.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append("%-12s: %s%n".formatted(e.getKey(), e.getValue())));
        return sb.toString();
    }
}
