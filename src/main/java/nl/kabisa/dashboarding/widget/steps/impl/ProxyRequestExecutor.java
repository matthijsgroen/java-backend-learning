package nl.kabisa.dashboarding.widget.steps.impl;

import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import nl.kabisa.dashboarding.widget.configuration.ProxyConfiguration;
import nl.kabisa.dashboarding.widget.dto.EndpointProcessingStep;
import nl.kabisa.dashboarding.widget.steps.StepExecutionContext;
import nl.kabisa.dashboarding.widget.steps.StepExecutionResult;
import nl.kabisa.dashboarding.widget.steps.StepExecutor;

@Component
public class ProxyRequestExecutor implements StepExecutor {

    private final RestClient restClient;
    private final Set<String> allowedSchemes;
    private final Set<String> allowedHosts;
    private static final Pattern PRIVATE_IP_PATTERN = Pattern.compile(
            "^(127\\.|10\\.|172\\.(1[6-9]|2[0-9]|3[01])\\.|192\\.168\\.|::1|fc[0-9a-f]{2}:).*");

    public ProxyRequestExecutor(ProxyConfiguration proxyConfiguration) {
        this.restClient = RestClient.create();
        this.allowedSchemes = new HashSet<>(proxyConfiguration.getAllowedSchemes());
        this.allowedHosts = new HashSet<>(proxyConfiguration.getAllowedHosts());
    }

    @Override
    public String action() {
        return "proxyRequest";
    }

    private void validateUrl(String urlString) throws IllegalArgumentException {
        try {
            URI uri = new URI(urlString);

            // Validate scheme
            if (!allowedSchemes.contains(uri.getScheme())) {
                throw new IllegalArgumentException("Scheme not allowed: " + uri.getScheme());
            }

            String host = uri.getHost();
            if (host == null || host.isEmpty()) {
                throw new IllegalArgumentException("Missing host in URL");
            }

            // Reject IP literals and private ranges
            if (PRIVATE_IP_PATTERN.matcher(host).find()) {
                throw new IllegalArgumentException("Private/internal IP address not allowed");
            }

            // Validate against allowlist
            if (!allowedHosts.contains(host)) {
                throw new IllegalArgumentException("Host not in allowlist: " + host);
            }
        } catch (java.net.URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL format", e);
        }
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
        validateUrl(resolvedUrl);

        try {
            ResponseEntity<byte[]> response = restClient
                    .method(org.springframework.http.HttpMethod.valueOf(method))
                    .uri(new URI(resolvedUrl))
                    .retrieve()
                    .toEntity(byte[].class);

            MediaType contentType = response.getHeaders().getContentType();
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM;
            }

            return new StepExecutionResult(
                    response.getBody(),
                    contentType,
                    response.getStatusCode());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Capture error response body and return it with error status code
            byte[] responseBody = e.getResponseBodyAsString().getBytes();
            MediaType contentType = MediaType.APPLICATION_JSON;
            if (e.getResponseHeaders() != null && e.getResponseHeaders().getContentType() != null) {
                contentType = e.getResponseHeaders().getContentType();
            }
            return new StepExecutionResult(responseBody, contentType, e.getStatusCode());
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute proxy request to " + resolvedUrl, e);
        }
    }
}