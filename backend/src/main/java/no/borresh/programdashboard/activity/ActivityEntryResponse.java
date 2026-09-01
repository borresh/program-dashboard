package no.borresh.programdashboard.activity;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import no.borresh.programdashboard.agent.AgentResponse;

public record ActivityEntryResponse(

        @Schema(requiredMode = REQUIRED)
        UUID id,

        @Schema(requiredMode = REQUIRED)
        ActivityType type,

        @Schema(description = "The agent the change is attributed to. Absent only for entries "
                + "written before an actor was recorded.")
        AgentResponse actor,

        @Schema(requiredMode = REQUIRED)
        String summary,

        @Schema(requiredMode = REQUIRED)
        Instant occurredAt) {

    public static ActivityEntryResponse from(ActivityEntry entry) {
        return new ActivityEntryResponse(
                entry.getId(),
                entry.getType(),
                entry.getActor() == null ? null : AgentResponse.from(entry.getActor()),
                entry.getSummary(),
                entry.getOccurredAt());
    }
}
