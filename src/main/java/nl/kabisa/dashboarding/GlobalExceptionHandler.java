package nl.kabisa.dashboarding;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import nl.kabisa.dashboarding.user.DuplicateEmailException;
import nl.kabisa.dashboarding.user.DuplicateUsernameException;
import nl.kabisa.dashboarding.user.SelfDemotionException;
import nl.kabisa.dashboarding.user.UserNotFoundException;
import nl.kabisa.dashboarding.widget.ConfigurationValidationException;
import nl.kabisa.dashboarding.widget.EndpointNotFoundException;
import nl.kabisa.dashboarding.widget.WidgetCycleException;
import nl.kabisa.dashboarding.widget.WidgetNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Generic handlers ─────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ValidationErrorResponse handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, List<String>> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();
            errors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(errorMessage);
        });

        ex.getBindingResult().getGlobalErrors().forEach(error -> {
            String objectName = error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            errors.computeIfAbsent(objectName, k -> new ArrayList<>()).add(errorMessage);
        });

        return new ValidationErrorResponse(
                HttpStatus.UNPROCESSABLE_CONTENT.value(),
                "Validation failed",
                errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleJsonParseError(HttpMessageNotReadableException ex) {
        return Map.of(
                "error", "Bad Request",
                "message", "Malformed JSON request body");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException ex) {
        return Map.of(
                "error", "Bad Request",
                "message", ex.getMessage());
    }

    // ── Widget module handlers ────────────────────────────────────────────

    @ExceptionHandler(ConfigurationValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ValidationErrorResponse handleConfigurationValidation(ConfigurationValidationException ex) {
        return new ValidationErrorResponse(
                HttpStatus.UNPROCESSABLE_CONTENT.value(),
                "Validation failed",
                ex.getErrors());
    }

    @ExceptionHandler(WidgetNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleWidgetNotFound(WidgetNotFoundException ex) {
        return Map.of(
                "error", "Not Found",
                "message", ex.getMessage());
    }

    @ExceptionHandler(EndpointNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleEndpointNotFound(EndpointNotFoundException ex) {
        return Map.of(
                "error", "Not Found",
                "message", ex.getMessage());
    }

    @ExceptionHandler(WidgetCycleException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleWidgetCycle(WidgetCycleException ex) {
        return Map.of(
                "error", "Conflict",
                "message", ex.getMessage());
    }

    // ── User module handlers ──────────────────────────────────────────────

    @ExceptionHandler(DuplicateUsernameException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleDuplicateUsername(DuplicateUsernameException ex) {
        return Map.of(
                "error", "Conflict",
                "message", ex.getMessage());
    }

    @ExceptionHandler(DuplicateEmailException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleDuplicateEmail(DuplicateEmailException ex) {
        return Map.of(
                "error", "Conflict",
                "message", ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleUserNotFound(UserNotFoundException ex) {
        return Map.of(
                "error", "Not Found",
                "message", ex.getMessage());
    }

    @ExceptionHandler(SelfDemotionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleSelfDemotion(SelfDemotionException ex) {
        return Map.of(
                "error", "Conflict",
                "message", ex.getMessage());
    }
}
