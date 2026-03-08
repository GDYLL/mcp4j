package io.github.mcp4j.transport;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.mcp4j.server.McpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * HTTP + Server-Sent Events (SSE) transport for remote MCP clients.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET  /sse}     — client subscribes; receives an {@code endpoint} event</li>
 *   <li>{@code POST /message} — client sends JSON-RPC; response is pushed via SSE</li>
 * </ul>
 *
 * <pre>{@code
 * McpServer.builder()
 *     .tools(new MyTools())
 *     .build()
 *     .start(new SseTransport(8080));
 * }</pre>
 */
public class SseTransport implements Transport {

    private static final Logger log = LoggerFactory.getLogger(SseTransport.class);

    private final int port;

    public SseTransport(int port) {
        this.port = port;
    }

    @Override
    public void start(McpHandler handler) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            SseSessionManager sessions = new SseSessionManager();

            server.createContext("/sse", exchange -> handleSse(exchange, sessions));
            server.createContext("/message", exchange -> handleMessage(exchange, handler, sessions));
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.start();

            log.info("MCP SSE server started on http://0.0.0.0:{}", port);
            Thread.currentThread().join(); // block until interrupted
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("MCP SSE server stopped");
        } catch (IOException e) {
            throw new RuntimeException("Failed to start SSE server on port " + port, e);
        }
    }

    private void handleSse(HttpExchange exchange, SseSessionManager sessions) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String sessionId = sessions.create(exchange);
        String endpointEvent = "event: endpoint\ndata: /message?sessionId=" + sessionId + "\n\n";

        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, 0);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(endpointEvent.getBytes(StandardCharsets.UTF_8));
            out.flush();

            // Keep connection open; client messages will be pushed to this stream
            sessions.park(sessionId);
        } finally {
            sessions.remove(sessionId);
        }
    }

    private void handleMessage(HttpExchange exchange, McpHandler handler,
                               SseSessionManager sessions) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String sessionId = queryParam(exchange.getRequestURI().getQuery(), "sessionId");
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(202, -1); // accepted

        String response = handler.handle(body);
        if (response != null && sessionId != null) {
            sessions.send(sessionId, response);
        }
    }

    private static String queryParam(String query, String name) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && name.equals(kv[0])) return kv[1];
        }
        return null;
    }

    /** Manages active SSE client connections. */
    private static class SseSessionManager {

        private final AtomicLong counter = new AtomicLong();
        private final ConcurrentHashMap<String, SseSession> sessions = new ConcurrentHashMap<>();

        String create(HttpExchange exchange) {
            String id = String.valueOf(counter.incrementAndGet());
            sessions.put(id, new SseSession(exchange));
            return id;
        }

        void park(String sessionId) {
            SseSession session = sessions.get(sessionId);
            if (session != null) session.park();
        }

        void send(String sessionId, String jsonResponse) {
            SseSession session = sessions.get(sessionId);
            if (session != null) session.send(jsonResponse);
        }

        void remove(String sessionId) {
            sessions.remove(sessionId);
        }
    }

    /** Holds a single SSE client connection and allows pushing messages to it. */
    private static class SseSession {

        private final HttpExchange exchange;
        private final Object lock = new Object();
        private volatile boolean closed = false;

        SseSession(HttpExchange exchange) {
            this.exchange = exchange;
        }

        void send(String json) {
            try {
                String event = "event: message\ndata: " + json + "\n\n";
                OutputStream out = exchange.getResponseBody();
                synchronized (lock) {
                    out.write(event.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    lock.notifyAll();
                }
            } catch (IOException e) {
                closed = true;
                synchronized (lock) { lock.notifyAll(); }
            }
        }

        void park() {
            synchronized (lock) {
                while (!closed) {
                    try { lock.wait(30_000); } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }
}
