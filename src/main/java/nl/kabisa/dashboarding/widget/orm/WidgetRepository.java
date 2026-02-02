package nl.kabisa.dashboarding.widget.orm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WidgetRepository extends JpaRepository<Widget, UUID> {
}
