package nl.kabisa.dashboarding.widget;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import nl.kabisa.dashboarding.widget.dto.DataEndpointModelItem;
import nl.kabisa.dashboarding.widget.orm.Widget;
import nl.kabisa.dashboarding.widget.orm.WidgetRepository;
import nl.kabisa.dashboarding.widget.steps.StepExecutionResult;
import nl.kabisa.dashboarding.widget.steps.StepExecutorService;

@RestController
public class WidgetEndpointsController {

    private final WidgetRepository widgetRepository;
    private final StepExecutorService stepExecutorService;

    public WidgetEndpointsController(WidgetRepository widgetRepository, StepExecutorService stepExecutorService) {
        this.widgetRepository = widgetRepository;
        this.stepExecutorService = stepExecutorService;
    }

    @GetMapping("/widget/{id}/endpoint/{endpointName}")
    public ResponseEntity<?> getWidget(@PathVariable String id,
            @PathVariable String endpointName) throws Exception {
        UUID widgetId;
        try {
            widgetId = UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid widget id", ex);
        }

        Widget widget = widgetRepository.findById(widgetId)
                .orElseThrow(() -> new WidgetNotFoundException(widgetId));

        List<DataEndpointModelItem> endpoints = widget.getEndpoints();
        Optional<DataEndpointModelItem> matchingEndpoint = endpoints.stream()
                .filter(e -> e.path().equals(endpointName))
                .findFirst();

        if (!matchingEndpoint.isPresent()) {
            throw new EndpointNotFoundException(endpointName);
        }

        DataEndpointModelItem endpoint = matchingEndpoint.get();

        StepExecutionResult result = stepExecutorService.executeSteps(endpoint.steps(), widget);
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(result.status());
        if (result.contentType() != null) {
            responseBuilder.contentType(result.contentType());
        }
        if (result.body() == null) {
            return responseBuilder.build();
        }
        return responseBuilder.body(result.body());
    }

}
