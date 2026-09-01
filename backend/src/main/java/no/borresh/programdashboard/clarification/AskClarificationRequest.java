package no.borresh.programdashboard.clarification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** R13. */
public record AskClarificationRequest(

        @NotNull(message = "must be the id of the registered agent asking the question")
        UUID askedByAgentId,

        @NotBlank(message = "must be the question being asked")
        String question,

        @Size(max = 4000, message = "must be at most 4000 characters")
        String context,

        @NotNull(message = "must be true or false; true marks the question as blocking the agent's work")
        Boolean blocking,

        @Valid
        List<ProposedOption> proposedOptions) {

    /**
     * One answer the asking agent is proposing, with why it thinks so. Order is kept as
     * submitted, so the agent can list its preferred option first.
     */
    public record ProposedOption(

            @NotBlank(message = "must be a short label for this option")
            @Size(max = 200, message = "must be at most 200 characters")
            String label,

            @NotBlank(message = "must explain why this option might be the right one")
            String rationale) {
    }
}
