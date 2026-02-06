package nl.kabisa.dashboarding.widget;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import nl.kabisa.dashboarding.widget.dto.DataEndpointModelItem;
import nl.kabisa.dashboarding.widget.orm.Widget;
import nl.kabisa.dashboarding.widget.orm.WidgetRepository;

@RestController
public class WidgetEndpointsController {

    private final WidgetRepository widgetRepository;

    public WidgetEndpointsController(WidgetRepository widgetRepository) {
        this.widgetRepository = widgetRepository;
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
        // For demonstration, we just return the endpoint configuration. In a real
        // implementation, you would execute the logic associated with this endpoint.
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "widgetId", widgetId.toString(),
                "endpoint", endpoint));
    }

}
