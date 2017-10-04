package ru.kontur.airlock;

import java.util.HashMap;
import java.util.Map;

public class AuthorizerFactory {
    private final HashMap<String, String[]> apiKeysToPatterns = new HashMap<>();

    public AuthorizerFactory(Map<String, String[]> apiKeysToRoutingKeyPatterns) {
        for (String key : apiKeysToRoutingKeyPatterns.keySet()) {
            String[] routingKeyPatterns = apiKeysToRoutingKeyPatterns.get(key);
            apiKeysToPatterns.put(key, routingKeyPatterns);
        }
    }

    public Authorizer getAuthorizer(String apiKey) {
        return new Authorizer(apiKeysToPatterns.get(apiKey));
    }
}
