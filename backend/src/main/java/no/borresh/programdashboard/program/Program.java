package no.borresh.programdashboard.program;

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

@Entity
@Table(name = "program")
public class Program {

    /** R6. Also enforced at the HTTP boundary so a bad slug is a 400, not a 500. */
    public static final String SLUG_PATTERN = "^[a-z0-9]+(-[a-z0-9]+)*$";

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "initial_prompt", nullable = false)
    private String initialPrompt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgramStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_agent_id", nullable = false)
    private Agent createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Program() {
        // for JPA
    }

    public Program(UUID id, String slug, String name, String description, String initialPrompt,
            Agent createdBy, Instant createdAt) {
        this.id = id;
        this.slug = slug;
        this.name = name;
        this.description = description;
        this.initialPrompt = initialPrompt;
        this.status = ProgramStatus.IDEA;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void describe(String description) {
        this.description = description;
    }

    public void replaceInitialPrompt(String initialPrompt) {
        this.initialPrompt = initialPrompt;
    }

    public void changeStatusTo(ProgramStatus status) {
        this.status = status;
    }

    /**
     * Bumped by any write to this program or to anything belonging to it, because the
     * overview sorts on it and a program with a fresh blocking question must not look
     * stale.
     */
    public void touch(Instant at) {
        this.updatedAt = at;
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getInitialPrompt() {
        return initialPrompt;
    }

    public ProgramStatus getStatus() {
        return status;
    }

    public Agent getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
