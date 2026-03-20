package nl.kabisa.dashboarding.widget;

public class WidgetAccessDeniedException extends RuntimeException {
    public WidgetAccessDeniedException() {
        super("Access denied");
    }
}
