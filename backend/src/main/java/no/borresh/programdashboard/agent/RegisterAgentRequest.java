package no.borresh.programdashboard.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * R1. Registration is idempotent by name, so an agent can send this at the start of every
 * session without checking whether it has registered before.
 */
public record RegisterAgentRequest(

        @NotBlank(message = "must be a non-empty name, for example 'opencode-implementer'")
        @Size(max = 100, message = "must be at most 100 characters")
        String name,

        @NotNull(message = "must be one of IMPLEMENTER, REVIEWER, HUMAN")
        AgentRole role,

        @Size(max = 500, message = "must be at most 500 characters")
        String description) {
}
