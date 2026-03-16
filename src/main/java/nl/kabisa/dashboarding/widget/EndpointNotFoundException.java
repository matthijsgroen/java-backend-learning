package nl.kabisa.dashboarding.widget;

public class EndpointNotFoundException extends RuntimeException {
    public EndpointNotFoundException(String path) {
        super("Data endpoint not found for widget: " + path);
    }
}
