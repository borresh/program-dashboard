package no.borresh.programdashboard.agent;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record AgentResponse(

        @Schema(requiredMode = REQUIRED)
        UUID id,

        @Schema(requiredMode = REQUIRED)
        String name,

        @Schema(requiredMode = REQUIRED)
        AgentRole role,

        String description,

        @Schema(requiredMode = REQUIRED)
        Instant registeredAt,

        @Schema(requiredMode = REQUIRED, description = "Last time this agent made a write that carried its id.")
        Instant lastSeenAt) {

    public static AgentResponse from(Agent agent) {
        return new AgentResponse(
                agent.getId(),
                agent.getName(),
                agent.getRole(),
                agent.getDescription(),
                agent.getRegisteredAt(),
                agent.getLastSeenAt());
    }
}
