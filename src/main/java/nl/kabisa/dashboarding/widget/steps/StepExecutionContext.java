package nl.kabisa.dashboarding.widget.steps;

import java.util.HashMap;
import java.util.Map;

import nl.kabisa.dashboarding.widget.orm.Widget;

public record StepExecutionContext(Widget widget, StepExecutionResult previousResult) {

    /**
     * Resolves placeholders in a template string by replacing %key% with values
     * from both frontend configuration and secrets configuration.
     * 
     * Frontend configuration values are checked first, then secrets configuration.
     * This allows frontend values to be overridden by secrets if needed.
     */
    @SuppressWarnings("unchecked")
    public String resolvePlaceholders(String template) {
        if (template == null || widget == null) {
            return template;
        }

        // Combine both configurations for placeholder resolution
        Map<String, Object> allValues = new HashMap<>();

        // Add frontend configuration values first (only if it's a Map)
        Object frontendConfig = widget.getFrontendConfiguration();
        if (frontendConfig instanceof Map) {
            allValues.putAll((Map<String, Object>) frontendConfig);
        }

        // Add secrets configuration values (these can override frontend values, only if
        // it's a Map)
        Object secretsConfig = widget.getSecretsConfiguration();
        if (secretsConfig instanceof Map) {
            allValues.putAll((Map<String, Object>) secretsConfig);
        }

        // Replace all placeholders
        String result = template;
        for (Map.Entry<String, Object> entry : allValues.entrySet()) {
            String placeholder = "%" + entry.getKey() + "%";
            if (entry.getValue() != null) {
                result = result.replace(placeholder, entry.getValue().toString());
            }
        }
        return result;
    }
}
