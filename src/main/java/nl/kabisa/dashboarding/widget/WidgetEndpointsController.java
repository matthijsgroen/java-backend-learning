package nl.kabisa.dashboarding.widget;

import org.springframework.web.bind.annotation.RestController;

import nl.kabisa.dashboarding.widget.orm.WidgetRepository;

@RestController
public class WidgetEndpointsController {

    private final WidgetRepository widgetRepository;

    public WidgetEndpointsController(WidgetRepository widgetRepository) {
        this.widgetRepository = widgetRepository;
    }

}
