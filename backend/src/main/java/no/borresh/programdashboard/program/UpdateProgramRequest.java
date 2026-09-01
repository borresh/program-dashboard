package no.borresh.programdashboard.program;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * R9. Every field except the actor is optional; an absent field is left unchanged.
 *
 * <p>{@code actorAgentId} is not in the prompt's field list but is required here, because
 * R19 wants an actor on every activity entry and a status change with no attributable
 * author is exactly the kind of log line that is useless a week later.
 */
public record UpdateProgramRequest(

        @NotNull(message = "must be the id of the registered agent making this change")
        UUID actorAgentId,

        @Size(min = 1, max = 200, message = "must be between 1 and 200 characters when present")
        String name,

        @Size(max = 2000, message = "must be at most 2000 characters when present")
        String description,

        @Size(min = 1, message = "must be non-empty when present")
        String initialPrompt,

        ProgramStatus status) {
}
