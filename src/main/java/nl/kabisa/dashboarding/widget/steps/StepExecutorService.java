package nl.kabisa.dashboarding.widget.steps;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import nl.kabisa.dashboarding.widget.dto.EndpointProcessingStep;
import nl.kabisa.dashboarding.widget.orm.Widget;

@Service
public class StepExecutorService {

    private final Map<String, StepExecutor> executorsByAction;

    public StepExecutorService(List<StepExecutor> executors) {
        this.executorsByAction = executors.stream()
                .collect(Collectors.toMap(
                        StepExecutor::action,
                        Function.identity()));
    }

    public StepExecutionResult executeSteps(List<EndpointProcessingStep> steps, Widget widget) {
        if (CollectionUtils.isEmpty(steps)) {
            throw new IllegalArgumentException("Endpoint has no steps to execute");
        }

        StepExecutionResult result = null;
        for (EndpointProcessingStep step : steps) {
            StepExecutor executor = executorsByAction.get(step.action());
            if (executor == null) {
                throw new StepExecutorNotFoundException(step.action());
            }
            result = executor.execute(step, new StepExecutionContext(widget, result));
        }

        return result;
    }
}
