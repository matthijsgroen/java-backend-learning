package nl.kabisa.dashboarding.widget.steps.impl;

import java.net.URI;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import nl.kabisa.dashboarding.widget.dto.EndpointProcessingStep;
import nl.kabisa.dashboarding.widget.steps.StepExecutionContext;
import nl.kabisa.dashboarding.widget.steps.StepExecutionResult;
import nl.kabisa.dashboarding.widget.steps.StepExecutor;

@Component
public class ProxyRequestExecutor implements StepExecutor {

    private final RestClient restClient;

    public ProxyRequestExecutor() {
        this.restClient = RestClient.create();
    }

    @Override
    public String action() {
        return "proxyRequest";
    }

    @Override
    public StepExecutionResult execute(EndpointProcessingStep step, StepExecutionContext context) {
        Map<String, Object> config = step.config();

        String method = (String) config.getOrDefault("method", "GET");
        String urlTemplate = (String) config.get("url");

        if (urlTemplate == null) {
            throw new IllegalArgumentException("proxyRequest step requires 'url' in config");
        }

        // Resolve placeholders from both frontend and secrets configuration
        String resolvedUrl = context.resolvePlaceholders(urlTemplate);

        try {
            byte[] responseBody = restClient
                    .method(org.springframework.http.HttpMethod.valueOf(method))
                    .uri(new URI(resolvedUrl))
                    .retrieve()
                    .body(byte[].class);

            return new StepExecutionResult(
                    responseBody,
                    MediaType.APPLICATION_JSON,
                    HttpStatus.OK);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute proxy request to " + resolvedUrl, e);
        }
    }
}