package edu.ppsu.devops;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class App {

    static final String VERSION =
            System.getenv().getOrDefault("APP_VERSION", "1.0");

    static final Map<String, AtomicLong> REQUESTS =
            new ConcurrentHashMap<>();

    static final long START = System.currentTimeMillis();

    static final String HOST =
            System.getenv().getOrDefault("HOSTNAME", "localhost");

    static final Calculator CALC = new Calculator();

    public static void main(String[] args) throws IOException {

        int port = Integer.parseInt(
                System.getenv().getOrDefault("PORT", "8080"));

        HttpServer server =
                HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", ex -> reply(ex, 200,
                "Hello from DevOps Demo - Main v" + VERSION +
                " (host: " + HOST + ")\n"));

        server.createContext("/health",
                ex -> reply(ex, 200, "OK\n"));

        server.createContext("/add", App::add);
        server.createContext("/metrics", App::metrics);

        server.start();

        System.out.println(
                "devops-demo v" + VERSION +
                " listening on port " + port);
    }

    static void add(HttpExchange ex) throws IOException {
        try {
            Map<String, Integer> q = new ConcurrentHashMap<>();

            String query = ex.getRequestURI().getQuery();

            for (String p : query.split("&")) {
                String[] kv = p.split("=");
                q.put(kv[0], Integer.parseInt(kv[1]));
            }

            reply(ex, 200,
                    CALC.add(q.get("a"), q.get("b")) + "\n");

        } catch (Exception e) {
            reply(ex, 400,
                    "Usage: /add?a=2&b=3\n");
        }
    }

    static void metrics(HttpExchange ex) throws IOException {

        StringBuilder sb = new StringBuilder();

        sb.append("# HELP app_info Application version info\n");
        sb.append("# TYPE app_info gauge\n");
        sb.append("app_info{version=\"")
                .append(VERSION)
                .append("\"} 1\n");

        sb.append("# HELP http_requests_total Total HTTP requests by path\n");
        sb.append("# TYPE http_requests_total counter\n");

        REQUESTS.forEach((path, n) ->
                sb.append("http_requests_total{path=\"")
                        .append(path)
                        .append("\"} ")
                        .append(n.get())
                        .append('\n'));

        Runtime rt = Runtime.getRuntime();

        sb.append("# HELP jvm_memory_used_bytes JVM heap memory in use\n");
        sb.append("# TYPE jvm_memory_used_bytes gauge\n");
        sb.append("jvm_memory_used_bytes ")
                .append(rt.totalMemory() - rt.freeMemory())
                .append('\n');

        sb.append("# HELP app_uptime_seconds Seconds since the application started\n");
        sb.append("# TYPE app_uptime_seconds gauge\n");
        sb.append("app_uptime_seconds ")
                .append((System.currentTimeMillis() - START) / 1000)
                .append('\n');

        reply(ex, 200, sb.toString());
    }

    static void reply(HttpExchange ex, int status, String body)
            throws IOException {

        String path = ex.getHttpContext().getPath();

        if (!path.equals("/metrics")) {
            REQUESTS.computeIfAbsent(
                    path,
                    k -> new AtomicLong()
            ).incrementAndGet();

            System.out.println(
                    ex.getRequestMethod() + " " +
                    ex.getRequestURI() + " -> " +
                    status);
        }

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        ex.getResponseHeaders().set(
                "Content-Type",
                "text/plain; charset=utf-8");

        ex.sendResponseHeaders(status, bytes.length);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}