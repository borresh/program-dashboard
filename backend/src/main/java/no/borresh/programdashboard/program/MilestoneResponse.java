package no.borresh.programdashboard.program;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import no.borresh.programdashboard.agent.AgentResponse;

public record MilestoneResponse(

        @Schema(requiredMode = REQUIRED)
        UUID id,

        @Schema(requiredMode = REQUIRED)
        String title,

        String description,

        @Schema(requiredMode = REQUIRED, description = "Position in the checklist, starting at 0.")
        int position,

        @Schema(requiredMode = REQUIRED)
        boolean complete,

        Instant completedAt,

        AgentResponse completedBy) {

    public static MilestoneResponse from(Milestone milestone) {
        return new MilestoneResponse(
                milestone.getId(),
                milestone.getTitle(),
                milestone.getDescription(),
                milestone.getSortOrder(),
                milestone.isComplete(),
                milestone.getCompletedAt(),
                milestone.getCompletedBy() == null ? null : AgentResponse.from(milestone.getCompletedBy()));
    }
}
