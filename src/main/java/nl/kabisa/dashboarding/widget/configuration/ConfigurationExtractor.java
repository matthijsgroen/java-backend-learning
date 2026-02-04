package nl.kabisa.dashboarding.widget.configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nl.kabisa.dashboarding.widget.dto.ConfigurationFieldScope;
import nl.kabisa.dashboarding.widget.dto.ConfigurationModelItem;

public final class ConfigurationExtractor {

    private ConfigurationExtractor() {
    }

    public static Map<String, Object> extractFrontend(Map<String, Object> configuration,
            List<ConfigurationModelItem> configurationModel) {
        return extractByScope(configuration, configurationModel, ConfigurationFieldScope.FRONTEND);
    }

    public static Map<String, Object> extractBackend(Map<String, Object> configuration,
            List<ConfigurationModelItem> configurationModel) {
        return extractByScope(configuration, configurationModel, ConfigurationFieldScope.BACKEND);
    }

    private static Map<String, Object> extractByScope(Map<String, Object> configuration,
            List<ConfigurationModelItem> configurationModel,
            ConfigurationFieldScope scope) {
        Map<String, Object> scoped = new LinkedHashMap<>();
        List<ConfigurationModelItem> modelItems = configurationModel == null ? List.of() : configurationModel;

        if (configuration == null) {
            return scoped;
        }

        for (ConfigurationModelItem item : modelItems) {
            if (item == null || item.scope() == null || item.scope() != scope) {
                continue;
            }
            if (configuration.containsKey(item.id())) {
                scoped.put(item.id(), configuration.get(item.id()));
            }
        }

        return scoped;
    }
}