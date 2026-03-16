package nl.kabisa.dashboarding.widget;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import nl.kabisa.dashboarding.widget.orm.Widget;

@RestController
public class WidgetController {

    private final WidgetService widgetService;

    public WidgetController(WidgetService widgetService) {
        this.widgetService = widgetService;
    }

    @PostMapping("/widget")
    public ResponseEntity<CreateWidgetResponse> createWidget(@Valid @RequestBody CreateWidgetRequest request) {
        Widget saved = widgetService.createWidget(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateWidgetResponse(saved.getId().toString(), "Widget created successfully"));
    }

    @GetMapping("/widget/{id}")
    public ResponseEntity<GetWidgetResponse> getWidget(@PathVariable String id) {
        UUID widgetId = parseWidgetId(id);
        WidgetService.WidgetWithChildren result = widgetService.getWidgetWithChildren(widgetId);
        Widget widget = result.widget();

        UUID parentId = widget.getParentId();
        GetWidgetResponse response = new GetWidgetResponse(
                widget.getId().toString(),
                widget.getWidgetType(),
                widget.getVersion(),
                widget.getFrontendConfiguration(),
                parentId != null ? parentId.toString() : null,
                result.childIds().stream().map(UUID::toString).toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/widget/{id}")
    public ResponseEntity<UpdateWidgetResponse> updateWidget(
            @PathVariable String id,
            @Valid @RequestBody UpdateWidgetRequest request) {
        UUID widgetId = parseWidgetId(id);
        Widget updated = widgetService.updateWidget(widgetId, request);
        return ResponseEntity.ok(
                new UpdateWidgetResponse(updated.getId().toString(), "Widget updated successfully"));
    }

    @DeleteMapping("/widget/{id}")
    public ResponseEntity<DeleteWidgetResponse> deleteWidget(@PathVariable String id) {
        UUID widgetId = parseWidgetId(id);
        int count = widgetService.deleteWidgetWithDescendants(widgetId);
        String message = count == 1
                ? "Widget deleted (no descendants)"
                : "Widget and " + (count - 1) + " descendant(s) deleted";
        return ResponseEntity.ok(new DeleteWidgetResponse(count, message));
    }

    @GetMapping("/widget/{id}/children")
    public ResponseEntity<List<WidgetChildSummary>> getWidgetChildren(@PathVariable String id) {
        UUID widgetId = parseWidgetId(id);
        List<Widget> children = widgetService.getChildren(widgetId);
        List<WidgetChildSummary> summaries = children.stream()
                .map(w -> {
                    UUID pid = w.getParentId();
                    return new WidgetChildSummary(
                            w.getId().toString(),
                            w.getWidgetType(),
                            w.getVersion(),
                            pid != null ? pid.toString() : null);
                })
                .toList();
        return ResponseEntity.ok(summaries);
    }

    private UUID parseWidgetId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid widget id", ex);
        }
    }
}
