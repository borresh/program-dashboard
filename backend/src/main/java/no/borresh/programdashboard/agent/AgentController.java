package no.borresh.programdashboard.agent;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "agents", description = "Registration and lookup of the agents that use this API.")
@RequestMapping(path = "/api/agents", produces = MediaType.APPLICATION_JSON_VALUE)
class AgentController {

    private final AgentService agentService;

    AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping
    @Operation(operationId = "registerAgent")
    ResponseEntity<AgentResponse> register(@Valid @RequestBody RegisterAgentRequest request) {
        AgentService.Registration registration = agentService.register(request);
        AgentResponse body = AgentResponse.from(registration.agent());

        return registration.created()
                ? ResponseEntity.created(URI.create("/api/agents/" + body.id())).body(body)
                : ResponseEntity.ok(body);
    }

    @GetMapping
    @Operation(operationId = "listAgents")
    List<AgentResponse> list() {
        return agentService.findAll();
    }
}
