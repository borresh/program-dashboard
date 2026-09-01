package no.borresh.programdashboard.clarification;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import no.borresh.programdashboard.agent.AgentResponse;

/** R16. What an implementing agent polls for, and what the dashboard renders. */
public record ClarificationResponse(

        @Schema(requiredMode = REQUIRED)
        UUID id,

        @Schema(requiredMode = REQUIRED)
        UUID programId,

        @Schema(requiredMode = REQUIRED)
        String programSlug,

        @Schema(requiredMode = REQUIRED)
        String question,

        String context,

        @Schema(requiredMode = REQUIRED, description = "Informational. The dashboard shows it prominently "
                + "but it never prevents any other operation.")
        boolean blocking,

        @Schema(requiredMode = REQUIRED)
        ClarificationStatus status,

        @Schema(requiredMode = REQUIRED)
        AgentResponse askedBy,

        @Schema(requiredMode = REQUIRED)
        Instant askedAt,

        @Schema(requiredMode = REQUIRED, description = "In the order the asking agent submitted them.")
        List<ClarificationOptionResponse> proposedOptions,

        String answerText,

        ClarificationOptionResponse chosenOption,

        AgentResponse answeredBy,

        Instant answeredAt) {

    public static ClarificationResponse from(Clarification clarification) {
        return new ClarificationResponse(
                clarification.getId(),
                clarification.getProgram().getId(),
                clarification.getProgram().getSlug(),
                clarification.getQuestion(),
                clarification.getContext(),
                clarification.isBlocking(),
                clarification.getStatus(),
                AgentResponse.from(clarification.getAskedBy()),
                clarification.getAskedAt(),
                clarification.getOptions().stream().map(ClarificationOptionResponse::from).toList(),
                clarification.getAnswerText(),
                clarification.getChosenOption() == null
                        ? null
                        : ClarificationOptionResponse.from(clarification.getChosenOption()),
                clarification.getAnsweredBy() == null
                        ? null
                        : AgentResponse.from(clarification.getAnsweredBy()),
                clarification.getAnsweredAt());
    }
}
