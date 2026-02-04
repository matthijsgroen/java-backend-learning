package nl.kabisa.dashboarding.widget;

import java.util.Map;

public record GetWidgetResponse(
        String id,
        String widgetType,
        Map<String, Object> configuration) {
}
