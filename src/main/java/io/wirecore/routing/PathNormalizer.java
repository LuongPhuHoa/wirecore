package io.wirecore.routing;

public final class PathNormalizer {
    private PathNormalizer() {}

    public static String normalize(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) return "/";

        String p = rawPath.trim();

        // Strip scheme/host if a full URL is passed.
        int schemeIdx = p.indexOf("://");
        if (schemeIdx >= 0) {
            int firstSlashAfterHost = p.indexOf('/', schemeIdx + 3);
            p = firstSlashAfterHost >= 0 ? p.substring(firstSlashAfterHost) : "/";
        }

        // Strip query string and fragment.
        int qIdx = p.indexOf('?');
        if (qIdx >= 0) p = p.substring(0, qIdx);
        int hashIdx = p.indexOf('#');
        if (hashIdx >= 0) p = p.substring(0, hashIdx);

        if (p.isEmpty()) return "/";
        if (p.charAt(0) != '/') p = "/" + p;

        while (p.length() > 1 && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }

        return p;
    }
}

