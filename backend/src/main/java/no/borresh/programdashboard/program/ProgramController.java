package no.borresh.programdashboard.program;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "programs", description = "Programs being tracked, and the milestones belonging to them.")
@RequestMapping(path = "/api/programs", produces = MediaType.APPLICATION_JSON_VALUE)
class ProgramController {

    private final ProgramService programService;
    private final MilestoneService milestoneService;

    ProgramController(ProgramService programService, MilestoneService milestoneService) {
        this.programService = programService;
        this.milestoneService = milestoneService;
    }

    @PostMapping
    @Operation(operationId = "createProgram")
    ResponseEntity<ProgramDetailResponse> create(@Valid @RequestBody CreateProgramRequest request) {
        ProgramDetailResponse created = programService.create(request);
        return ResponseEntity.created(URI.create("/api/programs/" + created.slug())).body(created);
    }

    @GetMapping
    @Operation(operationId = "listPrograms")
    List<ProgramSummaryResponse> list() {
        return programService.findAll();
    }

    @GetMapping("/{idOrSlug}")
    @Operation(operationId = "getProgram")
    ProgramDetailResponse findOne(@PathVariable String idOrSlug) {
        return programService.findOne(idOrSlug);
    }

    @PatchMapping("/{idOrSlug}")
    @Operation(operationId = "updateProgram")
    ProgramDetailResponse update(@PathVariable String idOrSlug,
            @Valid @RequestBody UpdateProgramRequest request) {
        return programService.update(idOrSlug, request);
    }

    @GetMapping("/{idOrSlug}/milestones")
    @Operation(operationId = "listMilestones")
    List<MilestoneResponse> listMilestones(@PathVariable String idOrSlug) {
        return milestoneService.findForProgram(idOrSlug);
    }

    @PostMapping("/{idOrSlug}/milestones")
    @Operation(operationId = "addMilestone")
    ResponseEntity<MilestoneResponse> addMilestone(@PathVariable String idOrSlug,
            @Valid @RequestBody AddMilestoneRequest request) {
        MilestoneResponse created = milestoneService.add(idOrSlug, request);
        return ResponseEntity.created(URI.create("/api/milestones/" + created.id())).body(created);
    }
}
