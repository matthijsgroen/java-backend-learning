package nl.kabisa.dashboarding.widget;

import java.util.List;
import java.util.Map;

public record ValidationErrorResponse(
        int status,
        String message,
        Map<String, List<String>> errors) {
}