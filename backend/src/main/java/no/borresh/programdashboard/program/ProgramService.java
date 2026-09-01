package no.borresh.programdashboard.program;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.borresh.programdashboard.activity.ActivityService;
import no.borresh.programdashboard.activity.ActivityType;
import no.borresh.programdashboard.agent.Agent;
import no.borresh.programdashboard.agent.AgentService;
import no.borresh.programdashboard.clarification.ClarificationService;
import no.borresh.programdashboard.common.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgramService {

    private final ProgramRepository programs;
    private final MilestoneRepository milestones;
    private final ProgramLookup programLookup;
    private final ClarificationService clarificationService;
    private final AgentService agentService;
    private final ActivityService activityService;
    private final Clock clock;

    ProgramService(ProgramRepository programs, MilestoneRepository milestones, ProgramLookup programLookup,
            ClarificationService clarificationService, AgentService agentService,
            ActivityService activityService, Clock clock) {
        this.programs = programs;
        this.milestones = milestones;
        this.programLookup = programLookup;
        this.clarificationService = clarificationService;
        this.agentService = agentService;
        this.activityService = activityService;
        this.clock = clock;
    }

    /** R4, R5. */
    @Transactional
    public ProgramDetailResponse create(CreateProgramRequest request) {
        Agent actor = agentService.requireActor(request.createdByAgentId());
        rejectDuplicateSlug(request.slug());

        Instant now = clock.instant();
        Program program = programs.save(new Program(UUID.randomUUID(), request.slug(), request.name(),
                request.description(), request.initialPrompt(), actor, now));

        List<Milestone> created = createInitialMilestones(program, request.milestones());

        activityService.record(program, ActivityType.PROGRAM_CREATED, actor,
                "Registered program '%s' with %d milestone(s)".formatted(program.getSlug(), created.size()));

        return detailOf(program, created);
    }

    /** R7. */
    @Transactional(readOnly = true)
    public List<ProgramSummaryResponse> findAll() {
        Map<String, ProgramRepository.ProgramCountsRow> countsByProgramId = programs.findAllCounts().stream()
                .collect(Collectors.toMap(ProgramRepository.ProgramCountsRow::getProgramId, row -> row));

        // Ordered here rather than in SQL: the ordering rule turns on a derived value, the
        // dataset is tens of rows, and this keeps the query readable.
        return programs.findAll().stream()
                .map(program -> ProgramSummaryResponse.of(program,
                        countsByProgramId.get(program.getId().toString())))
                .sorted(Comparator
                        .comparing((ProgramSummaryResponse summary) -> summary.blockingClarificationCount() == 0)
                        .thenComparing(ProgramSummaryResponse::updatedAt, Comparator.reverseOrder()))
                .toList();
    }

    /** R8. */
    @Transactional(readOnly = true)
    public ProgramDetailResponse findOne(String idOrSlug) {
        Program program = programLookup.require(idOrSlug);
        return detailOf(program);
    }

    /** R9. */
    @Transactional
    public ProgramDetailResponse update(String idOrSlug, UpdateProgramRequest request) {
        Agent actor = agentService.requireActor(request.actorAgentId());
        Program program = programLookup.require(idOrSlug);
        ProgramStatus previousStatus = program.getStatus();

        if (request.name() != null) {
            program.rename(request.name());
        }
        if (request.description() != null) {
            program.describe(request.description());
        }
        if (request.initialPrompt() != null) {
            program.replaceInitialPrompt(request.initialPrompt());
        }
        if (request.status() != null) {
            program.changeStatusTo(request.status());
        }
        program.touch(clock.instant());

        recordUpdate(program, actor, previousStatus, request);

        return detailOf(program);
    }

    private ProgramDetailResponse detailOf(Program program) {
        return detailOf(program, milestones.findByProgramIdOrderBySortOrderAscTitleAsc(program.getId()));
    }

    private ProgramDetailResponse detailOf(Program program, List<Milestone> programMilestones) {
        return ProgramDetailResponse.of(program, programMilestones,
                clarificationService.findForProgram(program.getId()));
    }

    private void rejectDuplicateSlug(String slug) {
        if (programs.existsBySlug(slug)) {
            Program existing = programs.findBySlug(slug).orElseThrow();
            throw new ConflictException("Slug already in use",
                    "Slug '%s' already belongs to program '%s' (id %s), created %s."
                            .formatted(slug, existing.getName(), existing.getId(), existing.getCreatedAt()));
        }
    }

    private List<Milestone> createInitialMilestones(Program program, List<CreateProgramRequest.NewMilestone> given) {
        if (given == null || given.isEmpty()) {
            return List.of();
        }
        List<Milestone> created = IntStream.range(0, given.size())
                .mapToObj(index -> new Milestone(UUID.randomUUID(), program, given.get(index).title(),
                        given.get(index).description(), index))
                .toList();
        return milestones.saveAll(created);
    }

    private void recordUpdate(Program program, Agent actor, ProgramStatus previousStatus,
            UpdateProgramRequest request) {
        if (request.status() != null && request.status() != previousStatus) {
            activityService.record(program, ActivityType.PROGRAM_STATUS_CHANGED, actor,
                    "Status changed from %s to %s".formatted(previousStatus, request.status()));
            return;
        }
        activityService.record(program, ActivityType.PROGRAM_UPDATED, actor,
                "Updated %s".formatted(changedFieldsOf(request)));
    }

    private static String changedFieldsOf(UpdateProgramRequest request) {
        List<String> changed = new ArrayList<>();
        if (request.name() != null) {
            changed.add("name");
        }
        if (request.description() != null) {
            changed.add("description");
        }
        if (request.initialPrompt() != null) {
            changed.add("initial prompt");
        }
        return changed.isEmpty() ? "nothing" : String.join(", ", changed);
    }
}
