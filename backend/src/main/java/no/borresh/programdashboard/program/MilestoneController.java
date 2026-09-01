package no.borresh.programdashboard.program;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "milestones", description = "Editing, completing and deleting individual milestones.")
@RequestMapping("/api/milestones")
class MilestoneController {

    private final MilestoneService milestoneService;

    MilestoneController(MilestoneService milestoneService) {
        this.milestoneService = milestoneService;
    }

    @PatchMapping("/{id}")
    MilestoneResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateMilestoneRequest request) {
        return milestoneService.update(id, request);
    }

    /**
     * The actor arrives as a query parameter rather than a body field, unlike the other
     * writes. A DELETE with a request body is legal but is dropped by some intermediaries
     * and generates awkwardly in OpenAPI clients; a visible query parameter is neither.
     */
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam UUID actorAgentId) {
        milestoneService.delete(id, actorAgentId);
        return ResponseEntity.noContent().build();
    }
}
