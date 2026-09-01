package no.borresh.programdashboard.program;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** R4. */
public record CreateProgramRequest(

        @NotBlank(message = "must be a non-empty slug")
        @Pattern(regexp = Program.SLUG_PATTERN,
                message = "must match ^[a-z0-9]+(-[a-z0-9]+)*$, for example 'program-dashboard'")
        @Size(max = 100, message = "must be at most 100 characters")
        String slug,

        @NotBlank(message = "must be a non-empty display name")
        @Size(max = 200, message = "must be at most 200 characters")
        String name,

        @Size(max = 2000, message = "must be at most 2000 characters")
        String description,

        @NotBlank(message = "must be the program's initial prompt, as markdown")
        String initialPrompt,

        @NotNull(message = "must be the id of a registered agent; register one with POST /api/agents")
        UUID createdByAgentId,

        @Valid
        List<NewMilestone> milestones) {

    /**
     * A milestone supplied while creating the program. Order in this list is the milestone
     * order, so there is no position to get wrong; a build-order list pasted from the
     * initial prompt is already in the right sequence.
     */
    public record NewMilestone(

            @NotBlank(message = "must be a non-empty title")
            @Size(max = 200, message = "must be at most 200 characters")
            String title,

            @Size(max = 2000, message = "must be at most 2000 characters")
            String description) {
    }
}
