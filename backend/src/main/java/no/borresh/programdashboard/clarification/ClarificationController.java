package no.borresh.programdashboard.clarification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
// Declared so the OpenAPI document says application/json rather than */*. Without it the
// generated client cannot tell the response is JSON and falls back to reading every body
// as a Blob, which no backend test can detect.
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "clarifications",
        description = "Questions an agent could not answer for itself, and the answers to them.")
class ClarificationController {

    private final ClarificationService clarificationService;

    ClarificationController(ClarificationService clarificationService) {
        this.clarificationService = clarificationService;
    }

    /** R13. */
    @PostMapping("/api/programs/{idOrSlug}/clarifications")
    @Operation(operationId = "askClarification")
    ResponseEntity<ClarificationResponse> ask(@PathVariable String idOrSlug,
            @Valid @RequestBody AskClarificationRequest request) {
        ClarificationResponse asked = clarificationService.ask(idOrSlug, request);
        return ResponseEntity.created(URI.create("/api/clarifications/" + asked.id())).body(asked);
    }

    /** R17. */
    @GetMapping("/api/clarifications")
    @Operation(operationId = "findClarifications")
    List<ClarificationResponse> find(@RequestParam(required = false) ClarificationStatus status,
            @RequestParam(required = false) String program) {
        return clarificationService.find(status, program);
    }

    /** R16. */
    @GetMapping("/api/clarifications/{id}")
    @Operation(operationId = "getClarification")
    ClarificationResponse findOne(@PathVariable UUID id) {
        return clarificationService.findOne(id);
    }

    /** R14, R15. */
    @PostMapping("/api/clarifications/{id}/answer")
    @Operation(operationId = "answerClarification")
    ClarificationResponse answer(@PathVariable UUID id,
            @Valid @RequestBody AnswerClarificationRequest request) {
        return clarificationService.answer(id, request);
    }
}
