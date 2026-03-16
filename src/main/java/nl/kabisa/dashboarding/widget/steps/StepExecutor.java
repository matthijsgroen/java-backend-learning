package nl.kabisa.dashboarding.widget.steps;

import nl.kabisa.dashboarding.widget.dto.EndpointProcessingStep;

public interface StepExecutor {
    String action();

    StepExecutionResult execute(EndpointProcessingStep step, StepExecutionContext context);
}
