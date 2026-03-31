package io.wirecore.routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Matches normalized paths against patterns like:
 * - "/users" (exact)
 * - "/users/:id" (parameterized)
 */
public final class PathTemplate {
    private final String pattern;
    private final List<String> segments;
    private final boolean parameterized;

    private PathTemplate(String pattern, List<String> segments, boolean parameterized) {
        this.pattern = pattern;
        this.segments = segments;
        this.parameterized = parameterized;
    }

    public static PathTemplate of(String rawPattern) {
        String normalized = PathNormalizer.normalize(rawPattern);
        List<String> segs = splitSegments(normalized);
        boolean param = segs.stream().anyMatch(PathTemplate::isParamSegment);
        return new PathTemplate(normalized, List.copyOf(segs), param);
    }

    public String pattern() {
        return pattern;
    }

    public boolean isParameterized() {
        return parameterized;
    }

    public Map<String, String> match(String rawPath) {
        String path = PathNormalizer.normalize(rawPath);
        List<String> pathSegs = splitSegments(path);
        if (pathSegs.size() != segments.size()) return null;

        if (!parameterized) {
            return pattern.equals(path) ? Map.of() : null;
        }

        Map<String, String> out = new HashMap<>();
        for (int i = 0; i < segments.size(); i++) {
            String pat = segments.get(i);
            String seg = pathSegs.get(i);

            if (isParamSegment(pat)) {
                String key = pat.substring(1);
                if (key.isEmpty()) return null;
                out.put(key, seg);
            } else if (!Objects.equals(pat, seg)) {
                return null;
            }
        }
        return Map.copyOf(out);
    }

    private static boolean isParamSegment(String segment) {
        return segment != null && segment.length() > 1 && segment.charAt(0) == ':';
    }

    private static List<String> splitSegments(String normalizedPath) {
        if ("/".equals(normalizedPath)) return List.of();
        String[] parts = normalizedPath.substring(1).split("/");
        List<String> out = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (!part.isEmpty()) out.add(part);
        }
        return out;
    }
}

