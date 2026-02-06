package nl.kabisa.dashboarding.widget;

import java.util.UUID;

public class WidgetNotFoundException extends RuntimeException {
    public WidgetNotFoundException(UUID id) {
        super("Widget not found: " + id);
    }
}
