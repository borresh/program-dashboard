package no.borresh.programdashboard.program;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import no.borresh.programdashboard.support.TestApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** R10, R11, R12, plus the milestone delete from the interface contract. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MilestoneApiTest {

    private final MockMvc mockMvc;
    private final JsonMapper objectMapper;
    private final TestApi api;

    private UUID agentId;

    MilestoneApiTest(MockMvc mockMvc, JsonMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.api = new TestApi(mockMvc, objectMapper);
    }

    @BeforeEach
    void registerActingAgentAndProgram() throws Exception {
        agentId = api.registerAgent("opencode-implementer", "IMPLEMENTER");
        api.createProgram("program-dashboard", agentId);
    }

    /** R10. */
    @Test
    void addingAMilestoneReturnsItAtTheRequestedPosition() throws Exception {
        mockMvc.perform(post("/api/programs/program-dashboard/milestones")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"actorAgentId": "%s", "title": "Skeleton",
                                 "description": "Compose, schema, health", "position": 0}"""
                                .formatted(agentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Skeleton"))
                .andExpect(jsonPath("$.position").value(0))
                .andExpect(jsonPath("$.complete").value(false))
                .andExpect(jsonPath("$.completedBy").doesNotExist());
    }

    /** R10, R12. */
    @Test
    void completingAMilestoneRecordsWhoDidItAndMovesProgress() throws Exception {
        UUID first = addMilestone("Skeleton", 0);
        addMilestone("Agents and programs", 1);

        mockMvc.perform(patch("/api/milestones/" + first)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"actorAgentId": "%s", "complete": true}""".formatted(agentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complete").value(true))
                .andExpect(jsonPath("$.completedAt").isNotEmpty())
                .andExpect(jsonPath("$.completedBy.name").value("opencode-implementer"));

        mockMvc.perform(get("/api/programs/program-dashboard"))
                .andExpect(jsonPath("$.completedMilestones").value(1))
                .andExpect(jsonPath("$.totalMilestones").value(2))
                .andExpect(jsonPath("$.progress").value(0.5));
    }

    /** R11. */
    @Test
    void completingACompletedMilestoneNamesWhoCompletedItAndWhen() throws Exception {
        UUID milestoneId = addMilestone("Skeleton", 0);
        String complete = """
                {"actorAgentId": "%s", "complete": true}""".formatted(agentId);

        mockMvc.perform(patch("/api/milestones/" + milestoneId)
                .contentType(APPLICATION_JSON).content(complete)).andExpect(status().isOk());

        mockMvc.perform(patch("/api/milestones/" + milestoneId)
                        .contentType(APPLICATION_JSON).content(complete))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Milestone already complete"))
                .andExpect(jsonPath("$.detail").value(allOf(
                        containsString("opencode-implementer"),
                        containsString(milestoneId.toString()))));
    }

    /** Reopening is deliberately not a conflict: undoing a mistake has to stay possible. */
    @Test
    void aCompletedMilestoneCanBeReopenedAndCompletedAgain() throws Exception {
        UUID milestoneId = addMilestone("Skeleton", 0);

        patchMilestone(milestoneId, """
                {"actorAgentId": "%s", "complete": true}""".formatted(agentId));
        patchMilestone(milestoneId, """
                {"actorAgentId": "%s", "complete": false}""".formatted(agentId))
                .andExpect(jsonPath("$.complete").value(false))
                .andExpect(jsonPath("$.completedBy").doesNotExist());
        patchMilestone(milestoneId, """
                {"actorAgentId": "%s", "complete": true}""".formatted(agentId))
                .andExpect(jsonPath("$.complete").value(true));
    }

    /** R10. Editing without touching completion. */
    @Test
    void patchingAMilestoneChangesOnlyWhatWasSent() throws Exception {
        UUID milestoneId = addMilestone("Skeleon", 0);

        patchMilestone(milestoneId, """
                {"actorAgentId": "%s", "title": "Skeleton"}""".formatted(agentId))
                .andExpect(jsonPath("$.title").value("Skeleton"))
                .andExpect(jsonPath("$.position").value(0))
                .andExpect(jsonPath("$.complete").value(false));
    }

    @Test
    void anUnknownMilestoneIsNamedInTheError() throws Exception {
        UUID unknown = UUID.fromString("22222222-2222-2222-2222-222222222222");

        mockMvc.perform(patch("/api/milestones/" + unknown)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"actorAgentId": "%s", "title": "x"}""".formatted(agentId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Milestone not found"))
                .andExpect(jsonPath("$.detail").value(containsString(unknown.toString())));
    }

    @Test
    void deletingAMilestoneRemovesItFromTheProgram() throws Exception {
        UUID milestoneId = addMilestone("Typo", 0);

        mockMvc.perform(delete("/api/milestones/" + milestoneId).param("actorAgentId", agentId.toString()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/programs/program-dashboard"))
                .andExpect(jsonPath("$.totalMilestones").value(0))
                .andExpect(jsonPath("$.progress").doesNotExist());
    }

    private UUID addMilestone(String title, int position) throws Exception {
        String body = """
                {"actorAgentId": "%s", "title": "%s", "position": %d}"""
                .formatted(agentId, title, position);

        String response = mockMvc.perform(post("/api/programs/program-dashboard/milestones")
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private ResultActions patchMilestone(UUID id, String body) throws Exception {
        return mockMvc.perform(patch("/api/milestones/" + id).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }
}
