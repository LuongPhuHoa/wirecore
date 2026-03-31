package io.wirecore.model;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class HttpResponse {
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private int statusCode = 200;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private byte[] body = new byte[0];

    public HttpResponse status(int code) {
        if (code < 100 || code > 599) throw new IllegalArgumentException("Invalid HTTP status: " + code);
        this.statusCode = code;
        return this;
    }

    public HttpResponse header(String key, String value) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Header name is required");
        if (value == null) throw new IllegalArgumentException("Header value is required");
        headers.put(key, value);
        return this;
    }

    public HttpResponse body(String body) {
        return body(body, DEFAULT_CHARSET);
    }

    public HttpResponse body(String body, Charset charset) {
        Objects.requireNonNull(charset, "charset");
        String s = body == null ? "" : body;
        this.body = s.getBytes(charset);
        if (!headers.containsKey("Content-Type")) {
            header("Content-Type", "text/plain; charset=" + charset.name().toLowerCase());
        }
        return this;
    }

    public int statusCode() {
        return statusCode;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public byte[] bodyBytes() {
        return body;
    }

    public byte[] toBytes() {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(statusCode).append(" ").append(reasonPhrase(statusCode)).append("\r\n");
        for (Map.Entry<String, String> e : headers.entrySet()) {
            sb.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
        }
        if (!headers.containsKey("Content-Length")) {
            sb.append("Content-Length: ").append(body.length).append("\r\n");
        }
        if (!headers.containsKey("Connection")) {
            sb.append("Connection: close\r\n");
        }
        sb.append("\r\n");

        byte[] head = sb.toString().getBytes(DEFAULT_CHARSET);
        byte[] out = new byte[head.length + body.length];
        System.arraycopy(head, 0, out, 0, head.length);
        System.arraycopy(body, 0, out, head.length, body.length);
        return out;
    }

    private static String reasonPhrase(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 202 -> "Accepted";
            case 204 -> "No Content";
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 304 -> "Not Modified";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 409 -> "Conflict";
            case 415 -> "Unsupported Media Type";
            case 422 -> "Unprocessable Content";
            case 500 -> "Internal Server Error";
            case 501 -> "Not Implemented";
            case 503 -> "Service Unavailable";
            default -> "";
        };
    }
}
