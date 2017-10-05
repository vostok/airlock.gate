package ru.kontur.airlock;

import java.util.HashMap;
import java.util.Map;

public class ValidatorFactory {
    private final HashMap<String, String[]> apiKeysToPatterns = new HashMap<>();

    public ValidatorFactory(Map<String, String[]> apiKeysToRoutingKeyPatterns) {
        for (String key : apiKeysToRoutingKeyPatterns.keySet()) {
            String[] routingKeyPatterns = apiKeysToRoutingKeyPatterns.get(key);
            apiKeysToPatterns.put(key, routingKeyPatterns);
        }
    }

    public Validator getValidator(String apiKey) {
        return new Validator(apiKeysToPatterns.get(apiKey));
    }
}
