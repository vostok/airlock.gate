package ru.kontur.airlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class AuthorizerFactory {
    private final HashMap<String, ArrayList<Pattern>> apiKeysToPatterns = new HashMap<>();

    public AuthorizerFactory(Map<String, String[]> apiKeysToRoutingKeyPatterns) {
        for (String key : apiKeysToRoutingKeyPatterns.keySet()) {
            String[] routingKeyPatterns = apiKeysToRoutingKeyPatterns.get(key);
            ArrayList<Pattern> patterns = new ArrayList<>();
            for (String routingKeyPattern : routingKeyPatterns) {
                Pattern pattern = Pattern.compile("^" + routingKeyPattern.trim().replace("*", ".*") + "$", Pattern.CASE_INSENSITIVE);
                patterns.add(pattern);
            }
            apiKeysToPatterns.put(key, patterns);
        }
    }

    public Authorizer getAuthorizer(String apiKey) {
        return new Authorizer(apiKeysToPatterns.get(apiKey));
    }
}
