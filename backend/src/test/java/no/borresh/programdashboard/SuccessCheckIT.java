package no.borresh.programdashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import no.borresh.programdashboard.support.PostgresContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The success check from the prompt, end to end, against a real PostgreSQL.
 *
 * <p>An implementing agent posts a clarification question through the REST API; it is
 * answered from the browser as the seeded HUMAN agent; the agent reads the answer back on
 * its next poll and continues. No copy-paste, nothing lost.
 *
 * <p>If this test fails, the product does not work, whatever else passes.
 */
@SpringBootTest(properties = "program-dashboard.cors.allowed-origins=http://localhost:4200")
@AutoConfigureMockMvc
@Import(PostgresContainerConfig.class)
class SuccessCheckIT {

    /** Seeded by V1__init.sql and handed to the browser as PROGRAM_DASHBOARD_HUMAN_AGENT_ID. */
    private static final UUID HUMAN_AGENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper;

    SuccessCheckIT(MockMvc mockMvc, JsonMapper jsonMapper) {
        this.mockMvc = mockMvc;
        this.jsonMapper = jsonMapper;
    }

    @Test
    void anAgentAsksAQuestionAndReadsBackTheAnswerIGaveInTheBrowser() throws Exception {
        // 1. The implementing agent registers itself. Idempotent, so it can do this every
        //    session without checking.
        JsonNode agent = postAndRead("/api/agents", """
                {"name": "opencode-implementer", "role": "IMPLEMENTER"}""", 201);
        String agentId = agent.get("id").asText();

        // 2. It registers the program from the prompt, before any code exists.
        postAndRead("/api/programs", """
                {"slug": "program-dashboard",
                 "name": "Program Dashboard",
                 "initialPrompt": "# program-dashboard\\n\\nA browser dashboard and REST API.",
                 "createdByAgentId": "%s",
                 "milestones": [{"title": "Skeleton"}, {"title": "Agents and programs"}]}"""
                .formatted(agentId), 201);

        // 3. It hits an ambiguity and asks, with two proposed answers.
        JsonNode asked = postAndRead("/api/programs/program-dashboard/clarifications", """
                {"askedByAgentId": "%s",
                 "question": "Can any registered agent complete a milestone, or only the agent that \
                 created the program?",
                 "context": "R10 records completedByAgentId but never says who is allowed to set it.",
                 "blocking": true,
                 "proposedOptions": [
                   {"label": "Any registered agent",
                    "rationale": "A second agent often picks up work mid-project, and \
                     completedByAgentId is already an audit trail."},
                   {"label": "Only the creating agent",
                    "rationale": "Prevents a stray script from marking work done."}]}"""
                .formatted(agentId), 201);

        String clarificationId = asked.get("id").asText();
        String firstOptionId = asked.get("proposedOptions").get(0).get("id").asText();
        assertThat(asked.get("status").asText()).isEqualTo("OPEN");
        assertThat(asked.get("blocking").asBoolean()).isTrue();

        // 4. The blocking question is visible on the overview, which is how I notice it.
        mockMvc.perform(get("/api/programs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug == 'program-dashboard')].blockingClarificationCount").value(1));

        // 5. I open /programs/program-dashboard, pick option 1 and submit. The browser sends
        //    the seeded HUMAN agent id it was given through PROGRAM_DASHBOARD_HUMAN_AGENT_ID.
        mockMvc.perform(post("/api/clarifications/" + clarificationId + "/answer")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"answeredByAgentId": "%s",
                                 "chosenOptionId": "%s",
                                 "answerText": "Option 1. completedByAgentId is enough of an audit trail."}"""
                                .formatted(HUMAN_AGENT_ID, firstOptionId)))
                .andExpect(status().isOk());

        // 6. The agent polls and gets its answer.
        mockMvc.perform(get("/api/clarifications/" + clarificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ANSWERED"))
                .andExpect(jsonPath("$.answerText")
                        .value("Option 1. completedByAgentId is enough of an audit trail."))
                .andExpect(jsonPath("$.chosenOption.label").value("Any registered agent"))
                .andExpect(jsonPath("$.answeredBy.name").value("harald"))
                .andExpect(jsonPath("$.answeredBy.role").value("HUMAN"))
                .andExpect(jsonPath("$.answeredAt").isNotEmpty());

        // 7. A second answer is rejected, naming who won and when.
        mockMvc.perform(post("/api/clarifications/" + clarificationId + "/answer")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"answeredByAgentId": "%s", "answerText": "Actually, option 2."}"""
                                .formatted(agentId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Clarification already answered"))
                .andExpect(jsonPath("$.detail").value(containsString("harald")));

        // 8. The program is no longer flagged as blocked, and the whole exchange is on the log.
        mockMvc.perform(get("/api/programs"))
                .andExpect(jsonPath("$[?(@.slug == 'program-dashboard')].blockingClarificationCount").value(0))
                .andExpect(jsonPath("$[?(@.slug == 'program-dashboard')].openClarificationCount").value(0));
    }

    private JsonNode postAndRead(String path, String body, int expectedStatus) throws Exception {
        String response = mockMvc.perform(post(path).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();

        return jsonMapper.readTree(response);
    }
}
