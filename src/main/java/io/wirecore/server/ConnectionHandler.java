package io.wirecore.server;

import io.wirecore.http.HttpMethod;
import io.wirecore.http.HttpRequest;
import io.wirecore.http.HttpRequestParser;
import io.wirecore.http.HttpResponse;
import io.wirecore.middleware.Middleware;
import io.wirecore.middleware.MiddlewareChain;
import io.wirecore.routing.RouteMatch;
import io.wirecore.routing.Router;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Handles a single TCP connection: parse → middleware chain → dispatch → respond.
 *
 * <p>The middleware chain wraps the route dispatch step, so every middleware
 * runs for all requests — including those that end in 404 or 405.
 */
public final class ConnectionHandler implements Runnable {
    private final Socket socket;
    private final Router router;
    private final HttpRequestParser requestParser;
    private final List<Middleware> middlewares;

    public ConnectionHandler(Socket socket, Router router, HttpRequestParser requestParser, List<Middleware> middlewares) {
        this.socket = socket;
        this.router = router;
        this.requestParser = requestParser;
        this.middlewares = middlewares;
    }

    public ConnectionHandler(Socket socket, Router router, HttpRequestParser requestParser) {
        this(socket, router, requestParser, List.of());
    }

    public ConnectionHandler(Socket socket, Router router) {
        this(socket, router, new HttpParser(), List.of());
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

            HttpResponse response = new HttpResponse();

            new MiddlewareChain(middlewares, (req, res) -> dispatch(req, res))
                    .execute(request, response);

            out.write(response.toBytes());
            out.flush();
        } catch (SocketException ex) {
            // Client reset / closed — ignore
        } catch (IOException ex) {
            System.err.println("Connection I/O error: " + ex.getMessage());
        }
    }

    /**
     * Terminal step: resolves the route and invokes the handler, or produces 404/405.
     */
    private void dispatch(HttpRequest request, HttpResponse response) {
        RouteMatch match = router.resolveMatch(request.method(), request.path());

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
