package no.borresh.programdashboard.program;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * R10. Edits a milestone or marks it complete. Every field except the actor is optional;
 * an absent field is left unchanged.
 *
 * <p>{@code complete} is tri-state on purpose: {@code true} completes, {@code false}
 * reopens, absent leaves the completion state alone.
 */
public record UpdateMilestoneRequest(

        @NotNull(message = "must be the id of the registered agent making this change")
        UUID actorAgentId,

        @Size(min = 1, max = 200, message = "must be between 1 and 200 characters when present")
        String title,

        @Size(max = 2000, message = "must be at most 2000 characters when present")
        String description,

        @PositiveOrZero(message = "must be zero or greater when present")
        Integer position,

        Boolean complete) {
}
