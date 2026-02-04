package nl.kabisa.dashboarding.widget;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
        Widget widget = new Widget();
        widget.setWidgetType(request.widgetType());
        widget.setConfiguration(request.configuration());
        widget.setConfigurationModel(request.configurationModel());
        widget.setEndpoints(request.dataEndpoints());

        Widget saved = widgetRepository.save(widget);
        CreateWidgetResponse response = new CreateWidgetResponse(saved.getId().toString(),
                "Widget created successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
