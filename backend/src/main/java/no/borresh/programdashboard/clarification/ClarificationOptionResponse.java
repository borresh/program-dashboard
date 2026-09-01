package no.borresh.programdashboard.clarification;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record ClarificationOptionResponse(

        @Schema(requiredMode = REQUIRED)
        UUID id,

        @Schema(requiredMode = REQUIRED, description = "Position in the list the asking agent submitted.")
        int position,

        @Schema(requiredMode = REQUIRED)
        String label,

        @Schema(requiredMode = REQUIRED, description = "Why the asking agent thinks this might be right.")
        String rationale) {

    public static ClarificationOptionResponse from(ClarificationOption option) {
        return new ClarificationOptionResponse(
                option.getId(), option.getSortOrder(), option.getLabel(), option.getRationale());
    }
}
