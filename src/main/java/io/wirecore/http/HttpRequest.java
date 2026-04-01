package io.wirecore.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class HttpRequest {
    private final HttpMethod method;
    private final String path;
    private final Map<String, String> queryParams;
    private final Map<String, String> pathParams;
    private final Map<String, String> headers;
    private final String body;

    public HttpRequest(
            HttpMethod method,
            String path,
            Map<String, String> queryParams,
            Map<String, String> pathParams,
            Map<String, String> headers,
            String body
    ) {
        this.method = Objects.requireNonNull(method, "method");
        this.path = Objects.requireNonNull(path, "path");
        this.queryParams = unmodifiableCopy(queryParams);
        this.pathParams = unmodifiableCopy(pathParams);
        this.headers = lowercaseKeysUnmodifiableCopy(headers);
        this.body = body == null ? "" : body;
    }

    public HttpMethod method() {
        return method;
    }

    public String path() {
        return path;
    }

    public Map<String, String> queryParams() {
        return queryParams;
    }

    public String queryParam(String key) {
        return queryParams.get(key);
    }

    public Map<String, String> pathParams() {
        return pathParams;
    }

    public String pathParam(String key) {
        return pathParams.get(key);
    }

    public Map<String, String> headers() {
        return headers;
    }

    public String header(String key) {
        if (key == null) return null;
        return headers.get(key.toLowerCase());
    }

    public String body() {
        return body;
    }

    /**
     * Returns a new request with path parameters merged in (routing layer).
     * Empty or null map returns this instance.
     */
    public HttpRequest withPathParams(Map<String, String> extraPathParams) {
        if (extraPathParams == null || extraPathParams.isEmpty()) {
            return this;
        }
        Map<String, String> merged = new HashMap<>(pathParams);
        merged.putAll(extraPathParams);
        return new HttpRequest(method, path, queryParams, merged, headers, body);
    }

    private static Map<String, String> unmodifiableCopy(Map<String, String> input) {
        if (input == null || input.isEmpty()) return Collections.emptyMap();
        return Collections.unmodifiableMap(new HashMap<>(input));
    }

    private static Map<String, String> lowercaseKeysUnmodifiableCopy(Map<String, String> input) {
        if (input == null || input.isEmpty()) return Collections.emptyMap();
        Map<String, String> out = new HashMap<>();
        for (Map.Entry<String, String> e : input.entrySet()) {
            if (e.getKey() == null) continue;
            out.put(e.getKey().toLowerCase(), e.getValue());
        }
        return Collections.unmodifiableMap(out);
    }
}
