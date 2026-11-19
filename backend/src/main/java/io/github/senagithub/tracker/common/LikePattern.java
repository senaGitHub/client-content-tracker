package io.github.senagithub.tracker.common;

import java.util.Locale;

/** Builds case-insensitive "contains" patterns for Criteria LIKE, using {@link #ESCAPE} as the escape char. */
public final class LikePattern {

    public static final char ESCAPE = '\\';

    private LikePattern() {
    }

    /** "50% off" becomes "%50\% off%", so user input never acts as a wildcard. */
    public static String containing(String value) {
        String escaped = value.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
