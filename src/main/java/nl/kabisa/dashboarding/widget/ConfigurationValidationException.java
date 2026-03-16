package nl.kabisa.dashboarding.widget;

import java.util.List;
import java.util.Map;

public class ConfigurationValidationException extends RuntimeException {
    private final Map<String, List<String>> errors;

    public ConfigurationValidationException(Map<String, List<String>> errors) {
        super("Configuration validation failed");
        this.errors = errors;
    }

    public Map<String, List<String>> getErrors() {
        return errors;
    }
}
