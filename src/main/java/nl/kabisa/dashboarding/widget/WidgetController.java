package nl.kabisa.dashboarding.widget;

import jakarta.validation.Valid;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import nl.kabisa.dashboarding.widget.configuration.ConfigurationExtractor;
import nl.kabisa.dashboarding.widget.configuration.ConfigurationValidator;
import nl.kabisa.dashboarding.widget.orm.Widget;
import nl.kabisa.dashboarding.widget.orm.WidgetRepository;

@RestController
public class WidgetController {

    private final WidgetRepository widgetRepository;

    public WidgetController(WidgetRepository widgetRepository) {
        this.widgetRepository = widgetRepository;
    }

    @PostMapping("/widget")
    public ResponseEntity<CreateWidgetResponse> createWidget(@Valid @RequestBody CreateWidgetRequest request) {
        ConfigurationValidator.validate(request.configuration(), request.configurationModel());
        Map<String, Object> frontendConfiguration = ConfigurationExtractor.extractFrontend(request.configuration(),
                request.configurationModel());
        Map<String, Object> backendConfiguration = ConfigurationExtractor.extractBackend(request.configuration(),
                request.configurationModel());

        Widget widget = new Widget();
        widget.setWidgetType(request.widgetType());

        widget.setFrontendConfiguration(frontendConfiguration);
        widget.setSecretsConfiguration(backendConfiguration);

        widget.setConfigurationModel(request.configurationModel());
        widget.setEndpoints(request.dataEndpoints());

        Widget saved = widgetRepository.save(widget);
        CreateWidgetResponse response = new CreateWidgetResponse(saved.getId().toString(),
                "Widget created successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
