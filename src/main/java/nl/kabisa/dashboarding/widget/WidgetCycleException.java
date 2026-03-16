package nl.kabisa.dashboarding.widget;

import java.util.UUID;

public class WidgetCycleException extends RuntimeException {
    public WidgetCycleException(UUID widgetId, UUID proposedParentId) {
        super("Setting parent " + proposedParentId +
                " for widget " + widgetId +
                " would create a cycle in the hierarchy");
    }
}
