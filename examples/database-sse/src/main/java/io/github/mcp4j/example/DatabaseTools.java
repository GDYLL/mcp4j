package io.github.mcp4j.example;

import io.github.mcp4j.annotation.McpParam;
import io.github.mcp4j.annotation.McpResource;
import io.github.mcp4j.annotation.McpTool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Example: in-memory "database" exposed as MCP tools and resources.
 *
 * <p>In a real application, replace the in-memory store with JDBC calls.
 */
public class DatabaseTools {

    private final Map<Integer, Map<String, String>> users = new ConcurrentHashMap<>();
    private final AtomicInteger idSeq = new AtomicInteger(1);

    public DatabaseTools() {
        // Seed some data
        insert("Alice", "alice@example.com");
        insert("Bob",   "bob@example.com");
        insert("Carol", "carol@example.com");
    }

    @McpTool(description = "Query users by name (partial match, case-insensitive)")
    public String queryUsers(
            @McpParam(value = "name", description = "Name keyword to search") String name) {
        List<String> results = new ArrayList<>();
        users.forEach((id, user) -> {
            if (user.get("name").toLowerCase().contains(name.toLowerCase())) {
                results.add("id=%d name=%s email=%s".formatted(id, user.get("name"), user.get("email")));
            }
        });
        return results.isEmpty() ? "No users found" : String.join("\n", results);
    }

    @McpTool(description = "Insert a new user and return the new user ID")
    public String insertUser(
            @McpParam(value = "name",  description = "Full name")    String name,
            @McpParam(value = "email", description = "Email address") String email) {
        int id = insert(name, email);
        return "Created user with id=" + id;
    }

    @McpTool(description = "Count total number of users")
    public String countUsers() {
        return "Total users: " + users.size();
    }

    @McpResource(
            uri         = "db://users/all",
            name        = "All Users",
            description = "Full dump of the users table",
            mimeType    = "text/plain"
    )
    public String allUsers() {
        StringBuilder sb = new StringBuilder("id | name  | email\n");
        sb.append("-".repeat(50)).append("\n");
        users.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append("%d  | %s | %s%n".formatted(
                        e.getKey(), e.getValue().get("name"), e.getValue().get("email"))));
        return sb.toString();
    }

    private int insert(String name, String email) {
        int id = idSeq.getAndIncrement();
        users.put(id, Map.of("name", name, "email", email));
        return id;
    }
}
