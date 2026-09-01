package no.borresh.programdashboard.activity;

import java.time.Clock;
import java.util.UUID;
import no.borresh.programdashboard.agent.Agent;
import no.borresh.programdashboard.program.Program;
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
    private final Clock clock;

    ActivityService(ActivityRepository entries, Clock clock) {
        this.entries = entries;
        this.clock = clock;
    }

    @Transactional
    public void record(Program program, ActivityType type, Agent actor, String summary) {
        entries.save(new ActivityEntry(UUID.randomUUID(), program, type, actor, summary, clock.instant()));
    }
}
