package nl.kabisa.dashboarding.widget;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import nl.kabisa.dashboarding.widget.dto.DataEndpointModelItem;
import nl.kabisa.dashboarding.widget.orm.Widget;
import nl.kabisa.dashboarding.widget.steps.StepExecutionResult;
import nl.kabisa.dashboarding.widget.steps.StepExecutorService;

@RestController
public class WidgetEndpointsController {

    private final WidgetService widgetService;
    private final StepExecutorService stepExecutorService;

    public WidgetEndpointsController(WidgetService widgetService, StepExecutorService stepExecutorService) {
        this.widgetService = widgetService;
        this.stepExecutorService = stepExecutorService;
    }

    @GetMapping("/widget/{id}/endpoint/{endpointName}")
    public ResponseEntity<?> getWidgetEndpoint(
            Authentication authentication,
            @PathVariable String id,
            @PathVariable String endpointName) throws Exception {
        UUID callerId = UUID.fromString(authentication.getName());
        UUID widgetId;
        try {
            widgetId = UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid widget id", ex);
        }

        // getWidget performs: findById (404 if missing) then assertIsOwner (403 if not owner)
        Widget widget = widgetService.getWidget(callerId, widgetId);

        List<DataEndpointModelItem> endpoints = Optional.ofNullable(widget.getEndpoints()).orElseGet(List::of);
        DataEndpointModelItem endpoint = endpoints.stream()
                .filter(e -> e.path().equals(endpointName))
                .findFirst()
                .orElseThrow(() -> new EndpointNotFoundException(endpointName));

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
