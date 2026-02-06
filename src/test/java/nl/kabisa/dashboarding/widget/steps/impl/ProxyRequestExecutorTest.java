package nl.kabisa.dashboarding.widget.steps.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.github.tomakehurst.wiremock.WireMockServer;

import nl.kabisa.dashboarding.widget.configuration.ProxyConfiguration;
import nl.kabisa.dashboarding.widget.dto.EndpointProcessingStep;
import nl.kabisa.dashboarding.widget.orm.Widget;
import nl.kabisa.dashboarding.widget.steps.StepExecutionContext;
import nl.kabisa.dashboarding.widget.steps.StepExecutionResult;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "dashboarding.proxy.allowed-schemes=http,https",
        "dashboarding.proxy.allowed-hosts=localhost,api.example.com,data.trusted-service.com"
})
class ProxyRequestExecutorTest {
    @Autowired
    private ProxyConfiguration proxyConfiguration;

    private WireMockServer wireMockServer;
    private ProxyRequestExecutor executor;
    private String baseUrl;

    @BeforeEach
    void setup() {
        wireMockServer = new WireMockServer();
        wireMockServer.start();
        baseUrl = "http://localhost:" + wireMockServer.port();
        executor = new ProxyRequestExecutor(proxyConfiguration);
    }

    @AfterEach
    void teardown() {
        wireMockServer.stop();
    }

    @Test
    void testProxyRequestWithGetMethod() {
        // Arrange
        wireMockServer.stubFor(
                get(urlEqualTo("/api/calendar/events"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"events\": []}")));

        Map<String, Object> config = new HashMap<>();
        config.put("method", "GET");
        config.put("url", baseUrl + "/api/calendar/events");

        EndpointProcessingStep step = new EndpointProcessingStep("proxyRequest", config);
        Widget widget = new Widget();
        StepExecutionContext context = new StepExecutionContext(widget, null);

        // Act
        StepExecutionResult result = executor.execute(step, context);

        // Assert
        assertThat(result.status()).isEqualTo(HttpStatus.OK);
        assertThat(result.contentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(result.body()).isEqualTo("{\"events\": []}".getBytes());
        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/calendar/events")));
    }

    @Test
    void testProxyRequestWithPlaceholderSubstitution() {
        // Arrange
        wireMockServer.stubFor(
                get(urlEqualTo("/ical/abcd1234"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "text/calendar")
                                .withBody("BEGIN:VCALENDAR")));

        Map<String, Object> config = new HashMap<>();
        config.put("method", "GET");
        config.put("url", baseUrl + "%secretPath%");

        EndpointProcessingStep step = new EndpointProcessingStep("proxyRequest", config);
        Widget widget = new Widget();
        Map<String, Object> secrets = new HashMap<>();
        secrets.put("secretPath", "/ical/abcd1234");
        widget.setSecretsConfiguration(secrets);

        StepExecutionContext context = new StepExecutionContext(widget, null);

        // Act
        StepExecutionResult result = executor.execute(step, context);

        // Assert
        assertThat(result.status()).isEqualTo(HttpStatus.OK);
        assertThat(result.contentType()).isEqualTo(MediaType.valueOf("text/calendar"));
        assertThat(result.body()).isEqualTo("BEGIN:VCALENDAR".getBytes());
        wireMockServer.verify(getRequestedFor(urlEqualTo("/ical/abcd1234")));
    }

    @Test
    void testProxyRequestWithFrontendConfigurationPlaceholder() {
        // Arrange
        wireMockServer.stubFor(
                get(urlEqualTo("/events"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"events\": []}")));

        Map<String, Object> config = new HashMap<>();
        config.put("method", "GET");
        config.put("url", baseUrl + "%path%");

        EndpointProcessingStep step = new EndpointProcessingStep("proxyRequest", config);
        Widget widget = new Widget();
        Map<String, Object> frontend = new HashMap<>();
        frontend.put("path", "/events");
        widget.setFrontendConfiguration(frontend);

        StepExecutionContext context = new StepExecutionContext(widget, null);

        // Act
        StepExecutionResult result = executor.execute(step, context);

        // Assert
        assertThat(result.status()).isEqualTo(HttpStatus.OK);
        assertThat(result.contentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(result.body()).isEqualTo("{\"events\": []}".getBytes());
        wireMockServer.verify(getRequestedFor(urlEqualTo("/events")));
    }

    @Test
    void testProxyRequestSecretConfigurationOverridesFrontend() {
        // Arrange - Secrets config should override frontend config for same key
        wireMockServer.stubFor(
                get(urlEqualTo("/secret/path"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"data\": \"secret\"}")));

        Map<String, Object> config = new HashMap<>();
        config.put("method", "GET");
        config.put("url", baseUrl + "%apiPath%");

        EndpointProcessingStep step = new EndpointProcessingStep("proxyRequest", config);
        Widget widget = new Widget();

        Map<String, Object> frontend = new HashMap<>();
        frontend.put("apiPath", "/public/path");
        widget.setFrontendConfiguration(frontend);

        Map<String, Object> secrets = new HashMap<>();
        secrets.put("apiPath", "/secret/path");
        widget.setSecretsConfiguration(secrets);

        StepExecutionContext context = new StepExecutionContext(widget, null);

        // Act
        StepExecutionResult result = executor.execute(step, context);

        // Assert - Should use secret value, not frontend value
        assertThat(result.status()).isEqualTo(HttpStatus.OK);
        assertThat(result.contentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(result.body()).isEqualTo("{\"data\": \"secret\"}".getBytes());
        wireMockServer.verify(getRequestedFor(urlEqualTo("/secret/path")));
    }

    @Test
    void testProxyRequestMissingUrl() {
        // Arrange
        Map<String, Object> config = new HashMap<>();
        config.put("method", "GET");
        // No "url" in config

        EndpointProcessingStep step = new EndpointProcessingStep("proxyRequest", config);
        Widget widget = new Widget();
        StepExecutionContext context = new StepExecutionContext(widget, null);

        // Act & Assert
        assertThatThrownBy(() -> executor.execute(step, context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("proxyRequest step requires 'url'");
    }

    @Test
    void testActionName() {
        assertThat(executor.action()).isEqualTo("proxyRequest");
    }

    @Test
    void testBlocksDisallowedScheme() {
        // Arrange - only http/https are allowed in test config
        Map<String, Object> config = new HashMap<>();
        config.put("method", "GET");
        config.put("url", "ftp://api.example.com/file.txt");

        EndpointProcessingStep step = new EndpointProcessingStep("proxyRequest", config);
        Widget widget = new Widget();
        StepExecutionContext context = new StepExecutionContext(widget, null);

        // Act & Assert
        assertThatThrownBy(() -> executor.execute(step, context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Scheme not allowed");
    }

    @Test
    void testBlocksPrivateIPLoopback() {
        // Arrange - localhost/127.0.0.1 should be blocked
        Map<String, Object> config = new HashMap<>();
        config.put("method", "GET");
        config.put("url", "https://127.0.0.1/admin");

        EndpointProcessingStep step = new EndpointProcessingStep("proxyRequest", config);
        Widget widget = new Widget();
        StepExecutionContext context = new StepExecutionContext(widget, null);

        // Act & Assert
        assertThatThrownBy(() -> executor.execute(step, context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Private/internal IP address not allowed");
    }

    @Test
    void testBlocksPrivateIPRange10() {
        // Arrange - 10.x.x.x range should be blocked
        Map<String, Object> config = new HashMap<>();
        config.put("method", "GET");
        config.put("url", "https://10.0.0.1/internal");

        EndpointProcessingStep step = new EndpointProcessingStep("proxyRequest", config);
        Widget widget = new Widget();
        StepExecutionContext context = new StepExecutionContext(widget, null);

        // Act & Assert
        assertThatThrownBy(() -> executor.execute(step, context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Private/internal IP address not allowed");
    }

    @Test
    void testBlocksPrivateIPRange192168() {
        // Arrange - 192.168.x.x range should be blocked
        Map<String, Object> config = new HashMap<>();
        config.put("method", "GET");
        config.put("url", "https://192.168.1.100/network");

        EndpointProcessingStep step = new EndpointProcessingStep("proxyRequest", config);
        Widget widget = new Widget();
        StepExecutionContext context = new StepExecutionContext(widget, null);

        // Act & Assert
        assertThatThrownBy(() -> executor.execute(step, context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Private/internal IP address not allowed");
    }

    @Test
    void testBlocksPrivateIPRange172() {
        // Arrange - 172.16.x.x to 172.31.x.x range should be blocked
        Map<String, Object> config = new HashMap<>();
        config.put("method", "GET");
        config.put("url", "https://172.16.0.1/docker");

        EndpointProcessingStep step = new EndpointProcessingStep("proxyRequest", config);
        Widget widget = new Widget();
        StepExecutionContext context = new StepExecutionContext(widget, null);

        // Act & Assert
        assertThatThrownBy(() -> executor.execute(step, context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Private/internal IP address not allowed");
    }

    @Test
    void testBlocksUnallowedHost() {
        // Arrange - host not in allowlist should be blocked
        Map<String, Object> config = new HashMap<>();
        config.put("method", "GET");
        config.put("url", "https://evilcorp.com/api");

        EndpointProcessingStep step = new EndpointProcessingStep("proxyRequest", config);
        Widget widget = new Widget();
        StepExecutionContext context = new StepExecutionContext(widget, null);

        // Act & Assert
        assertThatThrownBy(() -> executor.execute(step, context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Host not in allowlist");
    }
}
