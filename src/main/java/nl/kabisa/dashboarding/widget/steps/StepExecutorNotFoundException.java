package nl.kabisa.dashboarding.widget.steps;

public class StepExecutorNotFoundException extends RuntimeException {
    public StepExecutorNotFoundException(String action) {
        super("No step executor registered for action: " + action);
    }
}
