package no.borresh.programdashboard.program;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import no.borresh.programdashboard.agent.AgentResponse;
import no.borresh.programdashboard.clarification.ClarificationResponse;

/** R8. The whole program: its prompt, its milestones and its clarifications. */
public record ProgramDetailResponse(

        @Schema(requiredMode = REQUIRED)
        UUID id,

        @Schema(requiredMode = REQUIRED)
        String slug,

        @Schema(requiredMode = REQUIRED)
        String name,

        String description,

        @Schema(requiredMode = REQUIRED, description = "The prompt the program was registered from, as markdown.")
        String initialPrompt,

        @Schema(requiredMode = REQUIRED)
        ProgramStatus status,

        @Schema(requiredMode = REQUIRED)
        AgentResponse createdBy,

        @Schema(requiredMode = REQUIRED)
        Instant createdAt,

        @Schema(requiredMode = REQUIRED)
        Instant updatedAt,

        @Schema(requiredMode = REQUIRED)
        long completedMilestones,

        @Schema(requiredMode = REQUIRED)
        long totalMilestones,

        @Schema(description = "Completed over total, between 0 and 1. Absent when the program has no "
                + "milestones, which is different from 0% and must not be rendered as it.")
        Double progress,

        @Schema(requiredMode = REQUIRED)
        List<MilestoneResponse> milestones,

        @Schema(requiredMode = REQUIRED, description = "Open questions first, blocking ones above those.")
        List<ClarificationResponse> clarifications) {

    public static ProgramDetailResponse of(Program program, List<Milestone> milestones,
            List<ClarificationResponse> clarifications) {
        long completed = milestones.stream().filter(Milestone::isComplete).count();

        return new ProgramDetailResponse(
                program.getId(),
                program.getSlug(),
                program.getName(),
                program.getDescription(),
                program.getInitialPrompt(),
                program.getStatus(),
                AgentResponse.from(program.getCreatedBy()),
                program.getCreatedAt(),
                program.getUpdatedAt(),
                completed,
                milestones.size(),
                Progress.of(completed, milestones.size()),
                milestones.stream().map(MilestoneResponse::from).toList(),
                clarifications);
    }
}
