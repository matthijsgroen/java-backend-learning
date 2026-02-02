package nl.kabisa.dashboarding.widget;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WidgetController {

    @PostMapping("/widget")
    public ResponseEntity<CreateWidgetResponse> createWidget(@Valid @RequestBody CreateWidgetRequest request) {
        // Implementation to create a widget
        CreateWidgetResponse response = new CreateWidgetResponse("widget-123", "Widget created successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
