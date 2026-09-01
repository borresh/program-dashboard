package no.borresh.programdashboard.program;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProgramRepository extends JpaRepository<Program, UUID> {

    Optional<Program> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /**
     * R7. The four per-program counts the overview needs, in one query rather than two
     * lookups per row.
     *
     * <p>Native SQL because it aggregates over two child tables, one of which has no entity
     * yet. It selects nothing but an identifier and four counts on purpose: PostgreSQL and
     * H2 hand a {@code timestamp with time zone} back as different Java types, so any date
     * selected here would work under one engine and fail under the other. Everything except
     * the counts is read from the mapped entity instead, where Hibernate normalises the
     * type. The identifier is cast to varchar because a native query returns H2's uuid
     * column as a byte array.
     */
    @Query(value = """
            SELECT CAST(p.id AS varchar)                      AS "programId",
                   (SELECT COUNT(*) FROM milestone m
                     WHERE m.program_id = p.id)               AS "totalMilestones",
                   (SELECT COUNT(*) FROM milestone m
                     WHERE m.program_id = p.id
                       AND m.completed_at IS NOT NULL)        AS "completedMilestones",
                   (SELECT COUNT(*) FROM clarification c
                     WHERE c.program_id = p.id
                       AND c.status = 'OPEN')                 AS "openClarificationCount",
                   (SELECT COUNT(*) FROM clarification c
                     WHERE c.program_id = p.id
                       AND c.status = 'OPEN'
                       AND c.blocking = TRUE)                 AS "blockingClarificationCount"
              FROM program p
            """, nativeQuery = true)
    List<ProgramCountsRow> findAllCounts();

    /** Projection for {@link #findAllCounts()}. Counts only; see the note there. */
    interface ProgramCountsRow {
        String getProgramId();

        long getTotalMilestones();

        long getCompletedMilestones();

        long getOpenClarificationCount();

        long getBlockingClarificationCount();
    }
}
