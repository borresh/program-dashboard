package no.borresh.programdashboard.clarification;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClarificationOptionRepository extends JpaRepository<ClarificationOption, UUID> {
}
