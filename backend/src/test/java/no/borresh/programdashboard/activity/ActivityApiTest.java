package no.borresh.programdashboard.activity;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** R19, R20. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ActivityApiTest {

    private static final UUID SEEDED_HUMAN_AGENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper;
    private final TestApi api;

    private UUID agentId;

    ActivityApiTest(MockMvc mockMvc, JsonMapper jsonMapper) {
        this.mockMvc = mockMvc;
        this.jsonMapper = jsonMapper;
        this.api = new TestApi(mockMvc, jsonMapper);
    }

    @BeforeEach
    void registerActingAgent() throws Exception {
        agentId = api.registerAgent("opencode-implementer", "IMPLEMENTER");
    }

    /** R19. Every write appends exactly one entry, attributed to the agent that made it. */
    @Test
    void everyWriteAppendsAnAttributedEntryNewestFirst() throws Exception {
        api.createProgram("program-dashboard", agentId, "Skeleton");

        String milestoneId = mockMvc.perform(get("/api/programs/program-dashboard"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll("(?s).*\"milestones\":\\[\\{\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(patch("/api/milestones/" + milestoneId)
                .contentType(APPLICATION_JSON)
                .content("""
                        {"actorAgentId": "%s", "complete": true}""".formatted(agentId)));

        mockMvc.perform(patch("/api/programs/program-dashboard")
                .contentType(APPLICATION_JSON)
                .content("""
                        {"actorAgentId": "%s", "status": "ACTIVE"}""".formatted(agentId)));

        mockMvc.perform(get("/api/programs/program-dashboard/activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50))
                // Newest first.
                .andExpect(jsonPath("$.content[0].type").value("PROGRAM_STATUS_CHANGED"))
                .andExpect(jsonPath("$.content[0].summary").value(containsString("IDEA to ACTIVE")))
                .andExpect(jsonPath("$.content[0].actor.name").value("opencode-implementer"))
                .andExpect(jsonPath("$.content[1].type").value("MILESTONE_COMPLETED"))
                .andExpect(jsonPath("$.content[2].type").value("PROGRAM_CREATED"));
    }

    /** R19. Asking and answering are both on the log, and the answer names the human. */
    @Test
    void clarificationsAppearOnTheLogWithWhoAskedAndWhoAnswered() throws Exception {
        api.createProgram("program-dashboard", agentId);

        String clarificationId = mockMvc.perform(post("/api/programs/program-dashboard/clarifications")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"askedByAgentId": "%s", "question": "Which port?", "blocking": true}"""
                                .formatted(agentId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()
                .replaceAll("(?s).*?\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/clarifications/" + clarificationId + "/answer")
                .contentType(APPLICATION_JSON)
                .content("""
                        {"answeredByAgentId": "%s", "answerText": "4200"}"""
                        .formatted(SEEDED_HUMAN_AGENT_ID)));

        mockMvc.perform(get("/api/programs/program-dashboard/activity"))
                .andExpect(jsonPath("$.content[0].type").value("CLARIFICATION_ANSWERED"))
                .andExpect(jsonPath("$.content[0].actor.name").value("harald"))
                .andExpect(jsonPath("$.content[1].type").value("CLARIFICATION_ASKED"))
                .andExpect(jsonPath("$.content[1].summary").value(containsString("Blocking question")))
                .andExpect(jsonPath("$.content[1].actor.name").value("opencode-implementer"));
    }

    /** R20. */
    @Test
    void theLogIsPaged() throws Exception {
        api.createProgram("program-dashboard", agentId);
        for (int index = 0; index < 4; index++) {
            mockMvc.perform(post("/api/programs/program-dashboard/milestones")
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {"actorAgentId": "%s", "title": "M%d", "position": %d}"""
                            .formatted(agentId, index, index)));
        }

        mockMvc.perform(get("/api/programs/program-dashboard/activity")
                        .param("page", "0").param("size", "2"))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));

        mockMvc.perform(get("/api/programs/program-dashboard/activity")
                        .param("page", "2").param("size", "2"))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].type").value("PROGRAM_CREATED"));
    }

    @Test
    void theLogOfAnUnknownProgramIsA404NamingIt() throws Exception {
        mockMvc.perform(get("/api/programs/no-such-program/activity"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Program not found"))
                .andExpect(jsonPath("$.detail").value(containsString("no-such-program")));
    }
}
