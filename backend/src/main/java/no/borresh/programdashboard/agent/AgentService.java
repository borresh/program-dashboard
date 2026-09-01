package no.borresh.programdashboard.agent;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.borresh.programdashboard.common.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentService {

    private final AgentRepository agents;
    private final Clock clock;

    AgentService(AgentRepository agents, Clock clock) {
        this.agents = agents;
        this.clock = clock;
    }

    /**
     * R1. Returns the registered agent and whether it was created now, so the controller
     * can answer 201 for a new name and 200 for one that already existed.
     *
     * <p>Re-registering does not overwrite the stored role or description; it only records
     * that the agent is alive, since an agent sending this request is by definition seen.
     */
    @Transactional
    public Registration register(RegisterAgentRequest request) {
        Instant now = clock.instant();

        Optional<Agent> existing = agents.findByName(request.name());
        if (existing.isPresent()) {
            existing.get().markSeenAt(now);
            return new Registration(existing.get(), false);
        }

        Agent agent = new Agent(UUID.randomUUID(), request.name(), request.role(), request.description(), now);
        try {
            return new Registration(agents.saveAndFlush(agent), true);
        } catch (DataIntegrityViolationException raced) {
            // Two sessions registered the same name at once. Recovery is correct here
            // because registration is defined as idempotent by name: re-read and return
            // whichever insert won.
            Agent winner = agents.findByName(request.name())
                    .orElseThrow(() -> raced);
            winner.markSeenAt(now);
            return new Registration(winner, false);
        }
    }

    @Transactional(readOnly = true)
    public List<AgentResponse> findAll() {
        return agents.findAllByOrderByNameAsc().stream().map(AgentResponse::from).toList();
    }

    /**
     * R3. Resolves the agent that a write is attributed to and records that it was heard
     * from. Every mutating operation in the API funnels through here, which is what keeps
     * {@code lastSeenAt} meaningful and gives unknown agent ids a consistent 404.
     */
    @Transactional
    public Agent requireActor(UUID agentId) {
        Agent agent = agents.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found",
                        "No agent is registered with id %s. Register one with POST /api/agents first."
                                .formatted(agentId)));
        agent.markSeenAt(clock.instant());
        return agent;
    }

    /** An agent plus whether this call is what created it. */
    public record Registration(Agent agent, boolean created) {}
}
