package no.borresh.programdashboard.program;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import no.borresh.programdashboard.agent.Agent;
import no.borresh.programdashboard.common.ConflictException;

@Entity
@Table(name = "milestone")
public class Milestone {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Column(nullable = false)
    private String title;

    private String description;

    /**
     * Exposed through the API as {@code position}. The column is {@code sort_order} because
     * {@code position} is a function name in HQL and would be parsed as one.
     */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by_agent_id")
    private Agent completedBy;

    protected Milestone() {
        // for JPA
    }

    public Milestone(UUID id, Program program, String title, String description, int sortOrder) {
        this.id = id;
        this.program = program;
        this.title = title;
        this.description = description;
        this.sortOrder = sortOrder;
    }

    public boolean isComplete() {
        return completedAt != null;
    }

    /**
     * R11. Completing an already-complete milestone is a conflict that names who completed
     * it and when, rather than a silent no-op.
     */
    public void complete(Agent agent, Instant at) {
        if (isComplete()) {
            throw new ConflictException("Milestone already complete",
                    "Milestone %s ('%s') was completed by '%s' at %s."
                            .formatted(id, title, completedBy.getName(), completedAt));
        }
        this.completedAt = at;
        this.completedBy = agent;
    }

    /** Reopens a completed milestone. Not a conflict: undoing a mistake must stay possible. */
    public void reopen() {
        this.completedAt = null;
        this.completedBy = null;
    }

    public void retitle(String title) {
        this.title = title;
    }

    public void describe(String description) {
        this.description = description;
    }

    public void moveTo(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public UUID getId() {
        return id;
    }

    public Program getProgram() {
        return program;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Agent getCompletedBy() {
        return completedBy;
    }
}
