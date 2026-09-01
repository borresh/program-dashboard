package no.borresh.programdashboard.program;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilestoneRepository extends JpaRepository<Milestone, UUID> {

    List<Milestone> findByProgramIdOrderBySortOrderAscTitleAsc(UUID programId);

    long countByProgramId(UUID programId);
}
