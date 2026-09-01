package no.borresh.programdashboard.program;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** R10, adding a milestone to an existing program. */
public record AddMilestoneRequest(

        @NotNull(message = "must be the id of the registered agent adding this milestone")
        UUID actorAgentId,

        @NotBlank(message = "must be a non-empty title")
        @Size(max = 200, message = "must be at most 200 characters")
        String title,

        @Size(max = 2000, message = "must be at most 2000 characters")
        String description,

        @NotNull(message = "must be the milestone's position in the checklist, starting at 0")
        @PositiveOrZero(message = "must be zero or greater")
        Integer position) {
}
