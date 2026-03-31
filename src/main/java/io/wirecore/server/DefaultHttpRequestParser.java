package io.wirecore.server;

import io.wirecore.abstraction.HttpRequestParser;
import io.wirecore.model.HttpMethod;
import io.wirecore.model.HttpRequest;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses an HTTP/1.x request from a raw {@link InputStream}.
 * Body is read by byte count when {@code Content-Length} is present (not line-based).
 */
public final class DefaultHttpRequestParser implements HttpRequestParser {
    private static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;

    @Override
    public HttpRequest parse(InputStream input) throws IOException {
        String requestLine = readLine(input);
        if (requestLine == null || requestLine.isEmpty()) {
            // Common: TCP connect then close, or probe with no bytes — not a malformed HTTP message.
            throw new EOFException("empty_request_line");
        }

        String[] parts = requestLine.split("\\s+");
        if (parts.length < 3) {
            throw new IOException("Bad request line: " + requestLine);
        }

        HttpMethod method = HttpMethod.parse(parts[0]);
        if (method == null) {
            throw new IOException("Unsupported method: " + parts[0]);
        }

        String uri = parts[1];
        int q = uri.indexOf('?');
        String rawPath = q >= 0 ? uri.substring(0, q) : uri;
        String query = q >= 0 ? uri.substring(q + 1) : "";

        String path = urlDecodePath(rawPath);
        if (path.isEmpty()) {
            path = "/";
        }
        Map<String, String> queryParams = parseQueryString(query);

        Map<String, String> headers = new HashMap<>();
        while (true) {
            String line = readLine(input);
            if (line == null) {
                throw new EOFException("Unexpected end while reading headers");
            }
            if (line.isEmpty()) {
                break;
            }
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String name = line.substring(0, colon).trim().toLowerCase();
            String value = line.substring(colon + 1).trim();
            headers.put(name, value);
        }

        int contentLength = 0;
        String cl = headers.get("content-length");
        if (cl != null && !cl.isBlank()) {
            try {
                contentLength = Integer.parseInt(cl.trim());
            } catch (NumberFormatException ex) {
                throw new IOException("Invalid Content-Length: " + cl);
            }
            if (contentLength < 0) {
                throw new IOException("Invalid Content-Length: " + contentLength);
            }
        }

        String body = readBodyExact(input, contentLength);

        return new HttpRequest(method, path, queryParams, Map.of(), headers, body);
    }

    private static String readBodyExact(InputStream input, int length) throws IOException {
        if (length == 0) {
            return "";
        }
        byte[] buf = new byte[length];
        int off = 0;
        while (off < length) {
            int n = input.read(buf, off, length - off);
            if (n == -1) {
                throw new EOFException("Body shorter than Content-Length");
            }
            off += n;
        }
        return new String(buf, StandardCharsets.UTF_8);
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayBuilder buf = new ByteArrayBuilder(256);
        while (true) {
            int b = in.read();
            if (b == -1) {
                return buf.length() == 0 ? null : buf.toString(ISO_8859_1);
            }
            if (b == '\n') {
                break;
            }
            if (b == '\r') {
                int b2 = in.read();
                if (b2 == '\n') {
                    break;
                }
                buf.write('\r');
                if (b2 == -1) {
                    return buf.toString(ISO_8859_1);
                }
                buf.write(b2);
                continue;
            }
            buf.write(b);
        }
        return buf.toString(ISO_8859_1);
    }

    private static String urlDecodePath(String rawPath) {
        if (rawPath == null || rawPath.isEmpty()) {
            return "/";
        }
        try {
            return URLDecoder.decode(rawPath, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return rawPath;
        }
    }

    private static Map<String, String> parseQueryString(String query) {
        if (query == null || query.isEmpty()) {
            return Map.of();
        }
        Map<String, String> out = new HashMap<>();
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) continue;
            int eq = pair.indexOf('=');
            if (eq < 0) {
                String key = urlDecode(pair);
                if (!key.isEmpty()) out.putIfAbsent(key, "");
            } else {
                String key = urlDecode(pair.substring(0, eq));
                String val = urlDecode(pair.substring(eq + 1));
                if (!key.isEmpty()) out.putIfAbsent(key, val);
            }
        }
        return Map.copyOf(out);
    }

    private static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return s;
        }
    }

    private static final class ByteArrayBuilder {
        private byte[] data;
        private int len;

        ByteArrayBuilder(int initial) {
            this.data = new byte[initial];
        }

        void write(int b) {
            if (len >= data.length) {
                byte[] next = new byte[data.length * 2];
                System.arraycopy(data, 0, next, 0, len);
                data = next;
            }
            data[len++] = (byte) b;
        }

        int length() {
            return len;
        }

        String toString(Charset charset) {
            return new String(data, 0, len, charset);
        }
    }
}
