package no.borresh.programdashboard.clarification;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import no.borresh.programdashboard.activity.ActivityService;
import no.borresh.programdashboard.activity.ActivityType;
import no.borresh.programdashboard.agent.Agent;
import no.borresh.programdashboard.agent.AgentService;
import no.borresh.programdashboard.common.ConflictException;
import no.borresh.programdashboard.common.ResourceNotFoundException;
import no.borresh.programdashboard.program.Program;
import no.borresh.programdashboard.program.ProgramLookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClarificationService {

    private final ClarificationRepository clarifications;
    private final ClarificationOptionRepository options;
    private final ProgramLookup programLookup;
    private final AgentService agentService;
    private final ActivityService activityService;
    private final Clock clock;

    ClarificationService(ClarificationRepository clarifications, ClarificationOptionRepository options,
            ProgramLookup programLookup, AgentService agentService, ActivityService activityService,
            Clock clock) {
        this.clarifications = clarifications;
        this.options = options;
        this.programLookup = programLookup;
        this.agentService = agentService;
        this.activityService = activityService;
        this.clock = clock;
    }

    /** R13. */
    @Transactional
    public ClarificationResponse ask(String programIdOrSlug, AskClarificationRequest request) {
        Agent actor = agentService.requireActor(request.askedByAgentId());
        Program program = programLookup.require(programIdOrSlug);
        Instant now = clock.instant();

        Clarification clarification = clarifications.save(new Clarification(UUID.randomUUID(), program,
                request.question(), request.context(), request.blocking(), actor, now));
        clarification.proposeOptions(createOptions(clarification, request.proposedOptions()));

        program.touch(now);
        activityService.record(program, ActivityType.CLARIFICATION_ASKED, actor,
                "%sasked: %s".formatted(request.blocking() ? "Blocking question " : "Question ",
                        summarise(request.question())));

        return ClarificationResponse.from(clarification);
    }

    /**
     * R14, R15. The check and the write are one conditional update; the read below happens
     * only to explain a rejection, never to decide one.
     */
    @Transactional
    public ClarificationResponse answer(UUID clarificationId, AnswerClarificationRequest request) {
        Agent actor = agentService.requireActor(request.answeredByAgentId());
        Clarification clarification = require(clarificationId);
        ClarificationOption chosen = resolveChosenOption(clarification, request.chosenOptionId());

        int updated = clarifications.answerIfOpen(clarificationId, request.answerText(), chosen, actor,
                clock.instant(), ClarificationStatus.OPEN, ClarificationStatus.ANSWERED);

        if (updated == 0) {
            throw alreadyAnswered(require(clarificationId));
        }

        Clarification answered = require(clarificationId);
        answered.getProgram().touch(clock.instant());
        activityService.record(answered.getProgram(), ActivityType.CLARIFICATION_ANSWERED, actor,
                "Answered: %s".formatted(summarise(answered.getQuestion())));

        return ClarificationResponse.from(answered);
    }

    /** R16. The endpoint an implementing agent polls. */
    @Transactional(readOnly = true)
    public ClarificationResponse findOne(UUID clarificationId) {
        return ClarificationResponse.from(require(clarificationId));
    }

    /** R17. Both filters are optional. */
    @Transactional(readOnly = true)
    public List<ClarificationResponse> find(ClarificationStatus status, String programIdOrSlug) {
        UUID programId = programIdOrSlug == null ? null : programLookup.require(programIdOrSlug).getId();

        List<Clarification> found;
        if (programId != null && status != null) {
            found = clarifications.findByProgramIdAndStatusOrderByBlockingDescAskedAtDesc(programId, status);
        } else if (programId != null) {
            found = clarifications.findByProgramIdOrderByBlockingDescAskedAtDesc(programId);
        } else if (status != null) {
            found = clarifications.findByStatusOrderByBlockingDescAskedAtDesc(status);
        } else {
            found = clarifications.findAllByOrderByBlockingDescAskedAtDesc();
        }

        return found.stream().map(ClarificationResponse::from).toList();
    }

    /**
     * R8. Open questions first, blocking ones above those, so the detail page can render the
     * list as it comes.
     */
    @Transactional(readOnly = true)
    public List<ClarificationResponse> findForProgram(UUID programId) {
        return clarifications.findByProgramIdOrderByBlockingDescAskedAtDesc(programId).stream()
                .sorted((left, right) -> Boolean.compare(left.isAnswered(), right.isAnswered()))
                .map(ClarificationResponse::from)
                .toList();
    }

    private List<ClarificationOption> createOptions(Clarification clarification,
            List<AskClarificationRequest.ProposedOption> proposed) {
        if (proposed == null || proposed.isEmpty()) {
            return List.of();
        }
        return options.saveAll(IntStream.range(0, proposed.size())
                .mapToObj(index -> new ClarificationOption(UUID.randomUUID(), clarification, index,
                        proposed.get(index).label(), proposed.get(index).rationale()))
                .toList());
    }

    private ClarificationOption resolveChosenOption(Clarification clarification, UUID chosenOptionId) {
        if (chosenOptionId == null) {
            return null;
        }
        return clarification.getOptions().stream()
                .filter(option -> option.getId().equals(chosenOptionId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Option not found",
                        "Clarification %s has no proposed option with id %s. Its options are: %s."
                                .formatted(clarification.getId(), chosenOptionId,
                                        describeOptions(clarification))));
    }

    private static String describeOptions(Clarification clarification) {
        if (clarification.getOptions().isEmpty()) {
            return "none, so answer with answerText instead";
        }
        return clarification.getOptions().stream()
                .map(option -> "%s ('%s')".formatted(option.getId(), option.getLabel()))
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
    }

    private static ConflictException alreadyAnswered(Clarification clarification) {
        String answeredBy = Optional.ofNullable(clarification.getAnsweredBy())
                .map(Agent::getName)
                .orElse("an unknown agent");

        return new ConflictException("Clarification already answered",
                "Clarification %s was answered by '%s' at %s."
                        .formatted(clarification.getId(), answeredBy, clarification.getAnsweredAt()));
    }

    private Clarification require(UUID clarificationId) {
        return clarifications.findById(clarificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Clarification not found",
                        "No clarification exists with id %s.".formatted(clarificationId)));
    }

    private static String summarise(String question) {
        return question.length() <= 120 ? question : question.substring(0, 117) + "...";
    }
}
