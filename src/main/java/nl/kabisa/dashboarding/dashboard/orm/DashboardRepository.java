package nl.kabisa.dashboarding.dashboard.orm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DashboardRepository extends JpaRepository<Dashboard, UUID> {
    @Query("select new nl.kabisa.dashboarding.dashboard.orm.DashboardSummary(d.id, d.name, d.createdAt, d.modifiedAt) "
            +
            "from nl.kabisa.dashboarding.dashboard.orm.Dashboard d " +
            "where d.deletedAt is null " +
            "order by d.name asc")
    List<DashboardSummary> findAvailableSummaries();
}
