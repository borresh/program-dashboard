package no.borresh.programdashboard.program;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import no.borresh.programdashboard.activity.ActivityService;
import no.borresh.programdashboard.activity.ActivityType;
import no.borresh.programdashboard.agent.Agent;
import no.borresh.programdashboard.agent.AgentService;
import no.borresh.programdashboard.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MilestoneService {

    private final MilestoneRepository milestones;
    private final ProgramService programService;
    private final AgentService agentService;
    private final ActivityService activityService;
    private final Clock clock;

    MilestoneService(MilestoneRepository milestones, ProgramService programService, AgentService agentService,
            ActivityService activityService, Clock clock) {
        this.milestones = milestones;
        this.programService = programService;
        this.agentService = agentService;
        this.activityService = activityService;
        this.clock = clock;
    }

    /** R10. */
    @Transactional
    public MilestoneResponse add(String programIdOrSlug, AddMilestoneRequest request) {
        Agent actor = agentService.requireActor(request.actorAgentId());
        Program program = programService.require(programIdOrSlug);

        Milestone milestone = milestones.save(new Milestone(UUID.randomUUID(), program, request.title(),
                request.description(), request.position()));
        program.touch(clock.instant());

        activityService.record(program, ActivityType.MILESTONE_ADDED, actor,
                "Added milestone '%s'".formatted(milestone.getTitle()));

        return MilestoneResponse.from(milestone);
    }

    /** R10, R11. */
    @Transactional
    public MilestoneResponse update(UUID milestoneId, UpdateMilestoneRequest request) {
        Agent actor = agentService.requireActor(request.actorAgentId());
        Milestone milestone = require(milestoneId);
        Instant now = clock.instant();

        if (request.title() != null) {
            milestone.retitle(request.title());
        }
        if (request.description() != null) {
            milestone.describe(request.description());
        }
        if (request.position() != null) {
            milestone.moveTo(request.position());
        }
        applyCompletion(milestone, request, actor, now);

        milestone.getProgram().touch(now);

        if (!Boolean.TRUE.equals(request.complete())) {
            activityService.record(milestone.getProgram(), ActivityType.MILESTONE_UPDATED, actor,
                    "Updated milestone '%s'".formatted(milestone.getTitle()));
        }

        return MilestoneResponse.from(milestone);
    }

    /**
     * Not in R4-R18, but in the interface contract. A milestone typed in by mistake would
     * otherwise be permanent, and unlike a program it carries no history worth preserving.
     */
    @Transactional
    public void delete(UUID milestoneId, UUID actorAgentId) {
        Agent actor = agentService.requireActor(actorAgentId);
        Milestone milestone = require(milestoneId);
        Program program = milestone.getProgram();
        String title = milestone.getTitle();

        milestones.delete(milestone);
        program.touch(clock.instant());

        activityService.record(program, ActivityType.MILESTONE_DELETED, actor,
                "Deleted milestone '%s'".formatted(title));
    }

    @Transactional(readOnly = true)
    public List<MilestoneResponse> findForProgram(String programIdOrSlug) {
        Program program = programService.require(programIdOrSlug);
        return milestones.findByProgramIdOrderBySortOrderAscTitleAsc(program.getId()).stream()
                .map(MilestoneResponse::from)
                .toList();
    }

    private void applyCompletion(Milestone milestone, UpdateMilestoneRequest request, Agent actor, Instant now) {
        if (request.complete() == null) {
            return;
        }
        if (request.complete()) {
            // Throws R11's conflict if it is already complete.
            milestone.complete(actor, now);
            activityService.record(milestone.getProgram(), ActivityType.MILESTONE_COMPLETED, actor,
                    "Completed milestone '%s'".formatted(milestone.getTitle()));
        } else {
            milestone.reopen();
        }
    }

    private Milestone require(UUID milestoneId) {
        return milestones.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found",
                        "No milestone exists with id %s.".formatted(milestoneId)));
    }
}
