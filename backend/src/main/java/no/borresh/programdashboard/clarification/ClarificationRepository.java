package no.borresh.programdashboard.clarification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import no.borresh.programdashboard.agent.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClarificationRepository extends JpaRepository<Clarification, UUID> {

    List<Clarification> findByProgramIdOrderByBlockingDescAskedAtDesc(UUID programId);

    List<Clarification> findByProgramIdAndStatusOrderByBlockingDescAskedAtDesc(
            UUID programId, ClarificationStatus status);

    List<Clarification> findByStatusOrderByBlockingDescAskedAtDesc(ClarificationStatus status);

    List<Clarification> findAllByOrderByBlockingDescAskedAtDesc();

    /**
     * R15. Answers a clarification only if it is still open, and reports how many rows that
     * changed.
     *
     * <p>This is a conditional update rather than a read followed by a write. Two agents —
     * or the browser and an agent — answering at the same moment would both pass a
     * read-then-check, and the second write would silently overwrite the first answer. Here
     * the database decides: exactly one caller gets 1 back, everyone else gets 0 and is
     * told who won.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Clarification c
               SET c.status = :answered,
                   c.answerText = :answerText,
                   c.chosenOption = :chosenOption,
                   c.answeredBy = :answeredBy,
                   c.answeredAt = :answeredAt
             WHERE c.id = :id
               AND c.status = :open
            """)
    int answerIfOpen(
            @Param("id") UUID id,
            @Param("answerText") String answerText,
            @Param("chosenOption") ClarificationOption chosenOption,
            @Param("answeredBy") Agent answeredBy,
            @Param("answeredAt") Instant answeredAt,
            @Param("open") ClarificationStatus open,
            @Param("answered") ClarificationStatus answered);
}
