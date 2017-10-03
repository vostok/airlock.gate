package ru.kontur.airlock;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class Authorizer {
    private final ArrayList<Pattern> allowedRoutingKeyPatterns;

    public Authorizer(ArrayList<Pattern> allowedRoutingKeyPatterns) {
        this.allowedRoutingKeyPatterns = allowedRoutingKeyPatterns;
    }

    public boolean authorize(String eventRoutingKey) {
        if (allowedRoutingKeyPatterns != null) {
            for (Pattern pattern : allowedRoutingKeyPatterns) {
                if (pattern.matcher(eventRoutingKey).matches())
                    return true;
            }
        }
        return false;
    }
}
