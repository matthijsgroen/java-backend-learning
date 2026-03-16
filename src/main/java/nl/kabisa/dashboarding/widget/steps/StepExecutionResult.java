package nl.kabisa.dashboarding.widget.steps;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;

public record StepExecutionResult(byte[] body, MediaType contentType, HttpStatusCode status) {
}
