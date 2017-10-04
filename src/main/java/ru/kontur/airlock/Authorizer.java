package ru.kontur.airlock;

public class Authorizer {
    private final String[] allowedRoutingKeyPatterns;

    Authorizer(String[] allowedRoutingKeyPatterns) {
        this.allowedRoutingKeyPatterns = allowedRoutingKeyPatterns;
    }

    public boolean authorize(String eventRoutingKey) {
        if (allowedRoutingKeyPatterns != null) {
            for (String pattern : allowedRoutingKeyPatterns) {
                if (matches(eventRoutingKey, pattern))
                    return true;
            }
        }
        return false;
    }

    private boolean matches(String eventRoutingKey, String pattern) {
        if (pattern.endsWith("*"))
            return eventRoutingKey.toLowerCase().startsWith(pattern.substring(0, pattern.length() - 1).toLowerCase());
        else
            return eventRoutingKey.equalsIgnoreCase(pattern);
    }
}
