package no.borresh.programdashboard.program;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** R7. One row of the overview. */
public record ProgramSummaryResponse(

        @Schema(requiredMode = REQUIRED)
        UUID id,

        @Schema(requiredMode = REQUIRED)
        String slug,

        @Schema(requiredMode = REQUIRED)
        String name,

        @Schema(requiredMode = REQUIRED)
        ProgramStatus status,

        @Schema(requiredMode = REQUIRED)
        long completedMilestones,

        @Schema(requiredMode = REQUIRED)
        long totalMilestones,

        @Schema(description = "Completed over total, between 0 and 1. Absent when the program has no "
                + "milestones, which is different from 0% and must not be rendered as it.")
        Double progress,

        @Schema(requiredMode = REQUIRED)
        long openClarificationCount,

        @Schema(requiredMode = REQUIRED)
        long blockingClarificationCount,

        @Schema(requiredMode = REQUIRED)
        Instant updatedAt) {

    public static ProgramSummaryResponse of(Program program, ProgramRepository.ProgramCountsRow counts) {
        return new ProgramSummaryResponse(
                program.getId(),
                program.getSlug(),
                program.getName(),
                program.getStatus(),
                counts.getCompletedMilestones(),
                counts.getTotalMilestones(),
                Progress.of(counts.getCompletedMilestones(), counts.getTotalMilestones()),
                counts.getOpenClarificationCount(),
                counts.getBlockingClarificationCount(),
                program.getUpdatedAt());
    }
}
