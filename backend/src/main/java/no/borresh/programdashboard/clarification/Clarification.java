package no.borresh.programdashboard.clarification;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import no.borresh.programdashboard.agent.Agent;
import no.borresh.programdashboard.program.Program;

/**
 * A question an agent could not answer for itself.
 *
 * <p>There is deliberately no {@code answer(...)} method here. R15 requires the check and
 * the write to be one atomic conditional update guarded on {@code status = 'OPEN'}; doing
 * it in the entity would mean reading first and then writing, which is exactly the race
 * the requirement exists to prevent. See {@link ClarificationRepository#answerIfOpen}.
 */
@Entity
@Table(name = "clarification")
public class Clarification {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Column(nullable = false)
    private String question;

    private String context;

    @Column(nullable = false)
    private boolean blocking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClarificationStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asked_by_agent_id", nullable = false)
    private Agent askedBy;

    @Column(name = "asked_at", nullable = false)
    private Instant askedAt;

    @OneToMany(mappedBy = "clarification", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ClarificationOption> options = new ArrayList<>();

    @Column(name = "answer_text")
    private String answerText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chosen_option_id")
    private ClarificationOption chosenOption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_by_agent_id")
    private Agent answeredBy;

    @Column(name = "answered_at")
    private Instant answeredAt;

    protected Clarification() {
        // for JPA
    }

    public Clarification(UUID id, Program program, String question, String context, boolean blocking,
            Agent askedBy, Instant askedAt) {
        this.id = id;
        this.program = program;
        this.question = question;
        this.context = context;
        this.blocking = blocking;
        this.status = ClarificationStatus.OPEN;
        this.askedBy = askedBy;
        this.askedAt = askedAt;
    }

    /** Options are supplied once, at creation, in the order the asking agent listed them. */
    public void proposeOptions(List<ClarificationOption> proposed) {
        this.options = new ArrayList<>(proposed);
    }

    public boolean isAnswered() {
        return status == ClarificationStatus.ANSWERED;
    }

    public UUID getId() {
        return id;
    }

    public Program getProgram() {
        return program;
    }

    public String getQuestion() {
        return question;
    }

    public String getContext() {
        return context;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public ClarificationStatus getStatus() {
        return status;
    }

    public Agent getAskedBy() {
        return askedBy;
    }

    public Instant getAskedAt() {
        return askedAt;
    }

    public List<ClarificationOption> getOptions() {
        return List.copyOf(options);
    }

    public String getAnswerText() {
        return answerText;
    }

    public ClarificationOption getChosenOption() {
        return chosenOption;
    }

    public Agent getAnsweredBy() {
        return answeredBy;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }
}
