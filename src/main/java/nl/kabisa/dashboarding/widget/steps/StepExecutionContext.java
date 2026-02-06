package nl.kabisa.dashboarding.widget.steps;

import nl.kabisa.dashboarding.widget.orm.Widget;

public record StepExecutionContext(Widget widget, StepExecutionResult previousResult) {
}
