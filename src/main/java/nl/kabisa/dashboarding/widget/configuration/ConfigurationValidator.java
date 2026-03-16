package nl.kabisa.dashboarding.widget.configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nl.kabisa.dashboarding.widget.ConfigurationValidationException;
import nl.kabisa.dashboarding.widget.dto.ConfigurationFieldScope;
import nl.kabisa.dashboarding.widget.dto.ConfigurationFieldType;
import nl.kabisa.dashboarding.widget.dto.ConfigurationModelItem;

public final class ConfigurationValidator {

    private ConfigurationValidator() {
    }

    public static void validate(Map<String, Object> configuration, List<ConfigurationModelItem> configurationModel) {
        Map<String, List<String>> errors = new LinkedHashMap<>();

        List<ConfigurationModelItem> modelItems = configurationModel == null ? List.of() : configurationModel;
        Map<String, ConfigurationModelItem> modelById = new LinkedHashMap<>();

        for (ConfigurationModelItem item : modelItems) {
            if (item == null || item.id() == null || item.id().isBlank()) {
                addError(errors, "configurationModel", "Configuration model item id cannot be blank");
                continue;
            }
            if (modelById.containsKey(item.id())) {
                addError(errors, item.id(), "Duplicate configuration model item id");
                continue;
            }
            modelById.put(item.id(), item);
        }

        if (configuration != null) {
            for (String key : configuration.keySet()) {
                if (!modelById.containsKey(key)) {
                    addError(errors, key, "Unknown configuration key");
                }
            }
        }

        for (ConfigurationModelItem item : modelById.values()) {
            if (configuration == null || !configuration.containsKey(item.id())) {
                addError(errors, item.id(), "Missing configuration value");
                continue;
            }

            Object value = configuration.get(item.id());
            if (value == null) {
                addError(errors, item.id(), "Configuration value cannot be null");
                continue;
            }

            ConfigurationFieldType type = item.type();
            if (type == null) {
                addError(errors, item.id(), "Configuration type is required");
                continue;
            }

            if (!matchesType(value, type)) {
                addError(errors, item.id(), "Configuration value does not match type " + type);
                continue;
            }

            ConfigurationFieldScope scope = item.scope();
            if (scope == null) {
                addError(errors, item.id(), "Configuration scope is required");
            }
        }

        if (!errors.isEmpty()) {
            throw new ConfigurationValidationException(errors);
        }
    }

    private static boolean matchesType(Object value, ConfigurationFieldType type) {
        return switch (type) {
            case STRING -> value instanceof String;
            case INTEGER -> value instanceof Integer || value instanceof Long || (value instanceof Number
                    && ((Number) value).doubleValue() % 1 == 0);
            case BOOLEAN -> value instanceof Boolean;
        };
    }

    private static void addError(Map<String, List<String>> errors, String field, String message) {
        errors.computeIfAbsent(field, key -> new ArrayList<>()).add(message);
    }
}