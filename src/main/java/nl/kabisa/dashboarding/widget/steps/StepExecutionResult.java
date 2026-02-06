package nl.kabisa.dashboarding.widget.steps;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public record StepExecutionResult(byte[] body, MediaType contentType, HttpStatus status) {
}
