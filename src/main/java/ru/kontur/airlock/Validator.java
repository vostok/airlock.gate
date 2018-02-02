package ru.kontur.airlock;

import java.util.regex.Pattern;

public class Validator {
    private final String[] allowedRoutingKeyPatterns;
    private final Pattern allowedCharacters = Pattern.compile("[A-Za-z0-9.-]+");

    Validator(String[] allowedRoutingKeyPatterns) {
        this.allowedRoutingKeyPatterns = allowedRoutingKeyPatterns;
    }

    public boolean validate(String eventRoutingKey) {
        return validateForbiddenCharacters(eventRoutingKey) && validateApiKeyAccess(eventRoutingKey);
    }

    public boolean validateApiKey() {
        return allowedRoutingKeyPatterns != null;
    }

    private boolean validateForbiddenCharacters(String eventRoutingKey) {
        return allowedCharacters.matcher(eventRoutingKey).matches();
    }

    private boolean validateApiKeyAccess(String eventRoutingKey) {
        if (allowedRoutingKeyPatterns != null) {
            for (String pattern : allowedRoutingKeyPatterns) {
                if (matches(eventRoutingKey, pattern)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matches(String eventRoutingKey, String pattern) {
        if (pattern.endsWith("*")) {
            return eventRoutingKey.toLowerCase().startsWith(pattern.substring(0, pattern.length() - 1).toLowerCase());
        } else {
            return eventRoutingKey.equalsIgnoreCase(pattern);
        }
    }
}
