package no.borresh.programdashboard.clarification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * One answer an agent is proposing. Immutable once posted: an option whose wording changed
 * after it was chosen would make the answer unreadable a week later.
 */
@Entity
@Table(name = "clarification_option")
public class ClarificationOption {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clarification_id", nullable = false)
    private Clarification clarification;

    /** Exposed through the API as {@code position}; see the note in Milestone. */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private String rationale;

    protected ClarificationOption() {
        // for JPA
    }

    public ClarificationOption(UUID id, Clarification clarification, int sortOrder, String label,
            String rationale) {
        this.id = id;
        this.clarification = clarification;
        this.sortOrder = sortOrder;
        this.label = label;
        this.rationale = rationale;
    }

    public UUID getId() {
        return id;
    }

    public Clarification getClarification() {
        return clarification;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getLabel() {
        return label;
    }

    public String getRationale() {
        return rationale;
    }
}
