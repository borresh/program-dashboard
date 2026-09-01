package no.borresh.programdashboard.activity;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * R20. One page of the activity log.
 *
 * <p>An explicit record rather than Spring's {@code Page}: the framework's serialised shape
 * is not part of any contract, changes between versions, and generates awkwardly into a
 * typed client.
 */
public record ActivityPageResponse(

        @Schema(requiredMode = REQUIRED, description = "Newest first.")
        List<ActivityEntryResponse> content,

        @Schema(requiredMode = REQUIRED, description = "Zero-based page number.")
        int page,

        @Schema(requiredMode = REQUIRED)
        int size,

        @Schema(requiredMode = REQUIRED)
        long totalElements,

        @Schema(requiredMode = REQUIRED)
        int totalPages) {

    public static ActivityPageResponse from(Page<ActivityEntry> page) {
        return new ActivityPageResponse(
                page.getContent().stream().map(ActivityEntryResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
