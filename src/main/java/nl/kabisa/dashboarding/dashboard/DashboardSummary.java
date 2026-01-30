package nl.kabisa.dashboarding.dashboard;

import java.time.LocalDateTime;
import java.util.UUID;

public record DashboardSummary(UUID id, String name, LocalDateTime createdAt, LocalDateTime modifiedAt) {
}