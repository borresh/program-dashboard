package no.borresh.programdashboard.clarification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * R14. Either a written answer, a chosen option, or both. Neither is rejected at the HTTP
 * boundary rather than stored as an empty answer.
 */
public record AnswerClarificationRequest(

        @NotNull(message = "must be the id of the registered agent answering; from the browser this is "
                + "the seeded HUMAN agent")
        UUID answeredByAgentId,

        String answerText,

        UUID chosenOptionId) {

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "an answer needs at least one of answerText and chosenOptionId; both were absent")
    public boolean isAnswerPresent() {
        return answerText != null && !answerText.isBlank() || chosenOptionId != null;
    }
}
