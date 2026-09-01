package no.borresh.programdashboard.activity;

import java.time.Clock;
import java.util.UUID;
import no.borresh.programdashboard.agent.Agent;
import no.borresh.programdashboard.program.Program;
import no.borresh.programdashboard.program.ProgramLookup;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * R19. Appends to the activity log. Every write in the API calls this exactly once, inside
 * the same transaction as the change it describes, so the log cannot disagree with the
 * data.
 */
@Service
public class ActivityService {

    private final ActivityRepository entries;
    private final ProgramLookup programLookup;
    private final Clock clock;

    ActivityService(ActivityRepository entries, ProgramLookup programLookup, Clock clock) {
        this.entries = entries;
        this.programLookup = programLookup;
        this.clock = clock;
    }

    @Transactional
    public void record(Program program, ActivityType type, Agent actor, String summary) {
        entries.save(new ActivityEntry(UUID.randomUUID(), program, type, actor, summary, clock.instant()));
    }

    /** R20. Newest first, because the question is always "what just happened". */
    @Transactional(readOnly = true)
    public ActivityPageResponse findForProgram(String programIdOrSlug, int page, int size) {
        Program program = programLookup.require(programIdOrSlug);
        return ActivityPageResponse.from(
                entries.findByProgramIdOrderByOccurredAtDesc(program.getId(), PageRequest.of(page, size)));
    }
}
