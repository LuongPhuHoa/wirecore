package io.wirecore.http;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parses raw bytes into an {@link HttpRequest}.
 */
public interface HttpRequestParser {
    HttpRequest parse(InputStream input) throws IOException;
}
