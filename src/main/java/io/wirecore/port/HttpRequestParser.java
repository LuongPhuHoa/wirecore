package io.wirecore.port;

import io.wirecore.model.HttpRequest;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parses raw bytes into an {@link HttpRequest}.
 */
public interface HttpRequestParser {
    HttpRequest parse(InputStream input) throws IOException;
}
