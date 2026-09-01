package no.borresh.programdashboard.activity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import no.borresh.programdashboard.agent.Agent;
import no.borresh.programdashboard.program.Program;

/**
 * One line in a program's history. The log is append-only: this entity has no setters and
 * nothing in the application updates or deletes an entry.
 */
@Entity
@Table(name = "activity_entry")
public class ActivityEntry {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private ActivityType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_agent_id")
    private Agent actor;

    @Column(nullable = false)
    private String summary;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected ActivityEntry() {
        // for JPA
    }

    public ActivityEntry(UUID id, Program program, ActivityType type, Agent actor, String summary,
            Instant occurredAt) {
        this.id = id;
        this.program = program;
        this.type = type;
        this.actor = actor;
        this.summary = summary;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public Program getProgram() {
        return program;
    }

    public ActivityType getType() {
        return type;
    }

    public Agent getActor() {
        return actor;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
