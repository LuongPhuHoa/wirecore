package io.wirecore.server;

import io.wirecore.port.HttpRequestParser;
import io.wirecore.port.RouteResult;
import io.wirecore.port.Router;
import io.wirecore.model.HttpMethod;
import io.wirecore.model.HttpRequest;
import io.wirecore.model.HttpResponse;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.time.Instant;
import java.util.Set;

/**
 * Template method pattern: one connection → parse → dispatch → respond.
 */
public final class ConnectionHandler implements Runnable {
    private final Socket socket;
    private final Router router;
    private final HttpRequestParser requestParser;

    public ConnectionHandler(Socket socket, Router router, HttpRequestParser requestParser) {
        this.socket = socket;
        this.router = router;
        this.requestParser = requestParser;
    }

    public ConnectionHandler(Socket socket, Router router) {
        this(socket, router, new DefaultHttpRequestParser());
    }

    @Override
    public void run() {
        try (Socket s = socket) {
            s.setSoTimeout(30_000);
            s.setTcpNoDelay(true);
            InputStream in = s.getInputStream();
            OutputStream out = s.getOutputStream();

            HttpRequest request;
            try {
                request = requestParser.parse(in);
            } catch (EOFException ex) {
                if ("empty_request_line".equals(ex.getMessage())) {
                    // Idle connection or client closed before sending a request line — ignore.
                    return;
                }
                System.err.println("Bad request: " + ex.getMessage());
                writeJsonError(out, 400, "bad_request", "Bad Request", null, null);
                return;
            } catch (IOException ex) {
                System.err.println("Bad request: " + ex.getMessage());
                writeJsonError(out, 400, "bad_request", "Bad Request", null, null);
                return;
            }

            System.out.println(request.method() + " " + request.path() + " " + request.queryParams());

            RouteResult match = router.resolveResult(request.method(), request.path());
            HttpResponse response = new HttpResponse();

            if (match == null) {
                Set<HttpMethod> allowed = router.allowedMethods(request.path());
                if (!allowed.isEmpty()) {
                    response.status(405)
                            .header("Allow", joinAllow(allowed))
                            .header("Content-Type", "application/json; charset=utf-8")
                            .body(jsonError(405, "method_not_allowed", "Method Not Allowed", request.method().name(), request.path()));
                } else {
                    response.status(404)
                            .header("Content-Type", "application/json; charset=utf-8")
                            .body(jsonError(404, "not_found", "Not Found", request.method().name(), request.path()));
                }
            } else {
                HttpRequest reqWithParams = request.withPathParams(match.pathParams());
                try {
                    match.handler().handle(reqWithParams, response);
                } catch (RuntimeException ex) {
                    System.err.println("Handler error: " + ex.getMessage());
                    response.status(500)
                            .header("Content-Type", "application/json; charset=utf-8")
                            .body(jsonError(500, "internal_error", "Internal Server Error", request.method().name(), request.path()));
                }
            }

            out.write(response.toBytes());
            out.flush();
        } catch (SocketException ex) {
            // Client reset / closed — ignore
        } catch (IOException ex) {
            System.err.println("Connection I/O error: " + ex.getMessage());
        }
    }

    private static void writeJsonError(
            OutputStream out,
            int status,
            String code,
            String message,
            String method,
            String path
    ) throws IOException {
        HttpResponse r = new HttpResponse()
                .status(status)
                .header("Content-Type", "application/json; charset=utf-8")
                .body(jsonError(status, code, message, method, path));
        out.write(r.toBytes());
        out.flush();
    }

    private static String joinAllow(Set<HttpMethod> methods) {
        // Deterministic order (REST friendliness).
        StringBuilder sb = new StringBuilder();
        HttpMethod[] order = {
                HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH,
                HttpMethod.DELETE, HttpMethod.HEAD, HttpMethod.OPTIONS,
                HttpMethod.TRACE, HttpMethod.CONNECT
        };
        boolean first = true;
        for (HttpMethod m : order) {
            if (!methods.contains(m)) continue;
            if (!first) sb.append(", ");
            sb.append(m.name());
            first = false;
        }
        // If caller stored some non-standard method someday, fall back to set iteration:
        if (first) {
            for (HttpMethod m : methods) {
                if (!first) sb.append(", ");
                sb.append(m.name());
                first = false;
            }
        }
        return sb.toString();
    }

    private static String jsonError(int status, String code, String message, String method, String path) {
        // Small, dependency-free JSON.
        return "{"
                + "\"timestamp\":\"" + escapeJson(Instant.now().toString()) + "\","
                + "\"status\":" + status + ","
                + "\"error\":\"" + escapeJson(code) + "\","
                + "\"message\":\"" + escapeJson(message) + "\""
                + (method == null ? "" : ",\"method\":\"" + escapeJson(method) + "\"")
                + (path == null ? "" : ",\"path\":\"" + escapeJson(path) + "\"")
                + "}";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
