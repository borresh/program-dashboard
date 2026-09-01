package no.borresh.programdashboard.clarification;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** R13 to R18. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ClarificationApiTest {

    private static final UUID SEEDED_HUMAN_AGENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper;
    private final TestApi api;

    private UUID agentId;

    ClarificationApiTest(MockMvc mockMvc, JsonMapper jsonMapper) {
        this.mockMvc = mockMvc;
        this.jsonMapper = jsonMapper;
        this.api = new TestApi(mockMvc, jsonMapper);
    }

    @BeforeEach
    void registerActingAgentAndProgram() throws Exception {
        agentId = api.registerAgent("opencode-implementer", "IMPLEMENTER");
        api.createProgram("program-dashboard", agentId);
    }

    /** R13. */
    @Test
    void askingAQuestionOpensItAndKeepsTheProposedOptionsInOrder() throws Exception {
        mockMvc.perform(post("/api/programs/program-dashboard/clarifications")
                        .contentType(APPLICATION_JSON).content(askBody(true)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.blocking").value(true))
                .andExpect(jsonPath("$.askedBy.name").value("opencode-implementer"))
                .andExpect(jsonPath("$.programSlug").value("program-dashboard"))
                .andExpect(jsonPath("$.proposedOptions.length()").value(2))
                .andExpect(jsonPath("$.proposedOptions[0].position").value(0))
                .andExpect(jsonPath("$.proposedOptions[0].label").value("Any registered agent"))
                .andExpect(jsonPath("$.proposedOptions[1].position").value(1))
                .andExpect(jsonPath("$.proposedOptions[1].label").value("Only the creating agent"))
                .andExpect(jsonPath("$.answerText").doesNotExist())
                .andExpect(jsonPath("$.answeredBy").doesNotExist());
    }

    /** R13. Options are optional; a question can be pure prose. */
    @Test
    void aQuestionWithoutProposedOptionsIsAccepted() throws Exception {
        mockMvc.perform(post("/api/programs/program-dashboard/clarifications")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"askedByAgentId": "%s", "question": "Which port?", "blocking": false}"""
                                .formatted(agentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedOptions.length()").value(0));
    }

    /** R14. */
    @Test
    void answeringWithAChosenOptionRecordsWhoAnsweredAndWhat() throws Exception {
        JsonNode asked = ask(true);
        String optionId = asked.get("proposedOptions").get(0).get("id").asText();

        mockMvc.perform(post("/api/clarifications/" + asked.get("id").asText() + "/answer")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"answeredByAgentId": "%s", "chosenOptionId": "%s",
                                 "answerText": "Option 1. completedByAgentId is enough of an audit trail."}"""
                                .formatted(SEEDED_HUMAN_AGENT_ID, optionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ANSWERED"))
                .andExpect(jsonPath("$.chosenOption.label").value("Any registered agent"))
                .andExpect(jsonPath("$.answeredBy.name").value("harald"))
                .andExpect(jsonPath("$.answeredBy.role").value("HUMAN"))
                .andExpect(jsonPath("$.answeredAt").isNotEmpty());
    }

    /** R14. Free text alone is a complete answer. */
    @Test
    void answeringWithFreeTextAloneIsAccepted() throws Exception {
        JsonNode asked = ask(true);

        mockMvc.perform(post("/api/clarifications/" + asked.get("id").asText() + "/answer")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"answeredByAgentId": "%s", "answerText": "Neither. Do it the third way."}"""
                                .formatted(SEEDED_HUMAN_AGENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ANSWERED"))
                .andExpect(jsonPath("$.chosenOption").doesNotExist())
                .andExpect(jsonPath("$.answerText").value("Neither. Do it the third way."));
    }

    /** R14. Neither field is a 400, not a stored empty answer. */
    @Test
    void anAnswerWithNeitherTextNorChosenOptionIsRejected() throws Exception {
        JsonNode asked = ask(true);

        mockMvc.perform(post("/api/clarifications/" + asked.get("id").asText() + "/answer")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"answeredByAgentId": "%s"}""".formatted(SEEDED_HUMAN_AGENT_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.errors[0].expected").value(allOf(
                        containsString("answerText"),
                        containsString("chosenOptionId"))));
    }

    /** R14. A blank answerText is not an answer either. */
    @Test
    void anAnswerWithOnlyWhitespaceIsRejected() throws Exception {
        JsonNode asked = ask(true);

        mockMvc.perform(post("/api/clarifications/" + asked.get("id").asText() + "/answer")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"answeredByAgentId": "%s", "answerText": "   "}"""
                                .formatted(SEEDED_HUMAN_AGENT_ID)))
                .andExpect(status().isBadRequest());
    }

    /** R15. */
    @Test
    void answeringAnAnsweredClarificationNamesWhoAnsweredItAndWhen() throws Exception {
        JsonNode asked = ask(true);
        String clarificationId = asked.get("id").asText();
        String answer = """
                {"answeredByAgentId": "%s", "answerText": "The first answer."}"""
                .formatted(SEEDED_HUMAN_AGENT_ID);

        mockMvc.perform(post("/api/clarifications/" + clarificationId + "/answer")
                .contentType(APPLICATION_JSON).content(answer)).andExpect(status().isOk());

        mockMvc.perform(post("/api/clarifications/" + clarificationId + "/answer")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"answeredByAgentId": "%s", "answerText": "A second answer."}"""
                                .formatted(agentId)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Clarification already answered"))
                .andExpect(jsonPath("$.detail").value(allOf(
                        containsString(clarificationId),
                        containsString("harald"))));

        // The first answer is intact; the loser of the race changed nothing.
        mockMvc.perform(get("/api/clarifications/" + clarificationId))
                .andExpect(jsonPath("$.answerText").value("The first answer."))
                .andExpect(jsonPath("$.answeredBy.name").value("harald"));
    }

    /** R14. An option belonging to a different clarification is named, not silently ignored. */
    @Test
    void choosingAnOptionFromAnotherClarificationIsRejected() throws Exception {
        JsonNode first = ask(true);
        JsonNode second = ask(false);
        String foreignOptionId = second.get("proposedOptions").get(0).get("id").asText();

        mockMvc.perform(post("/api/clarifications/" + first.get("id").asText() + "/answer")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"answeredByAgentId": "%s", "chosenOptionId": "%s"}"""
                                .formatted(SEEDED_HUMAN_AGENT_ID, foreignOptionId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Option not found"))
                .andExpect(jsonPath("$.detail").value(containsString(foreignOptionId)));
    }

    /** R16. */
    @Test
    void anUnknownClarificationIsNamedInTheError() throws Exception {
        UUID unknown = UUID.fromString("33333333-3333-3333-3333-333333333333");

        mockMvc.perform(get("/api/clarifications/" + unknown))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Clarification not found"))
                .andExpect(jsonPath("$.detail").value(containsString(unknown.toString())));
    }

    /** R17. Both filters are optional and combine. */
    @Test
    void clarificationsCanBeFilteredByStatusAndProgram() throws Exception {
        JsonNode open = ask(true);
        JsonNode toAnswer = ask(false);
        mockMvc.perform(post("/api/clarifications/" + toAnswer.get("id").asText() + "/answer")
                .contentType(APPLICATION_JSON)
                .content("""
                        {"answeredByAgentId": "%s", "answerText": "done"}"""
                        .formatted(SEEDED_HUMAN_AGENT_ID)));

        mockMvc.perform(get("/api/clarifications").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(open.get("id").asText()));

        mockMvc.perform(get("/api/clarifications").param("program", "program-dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/clarifications")
                        .param("status", "OPEN").param("program", "program-dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/clarifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    /** R18. Blocking is informational: it never prevents anything. */
    @Test
    void aBlockingQuestionDoesNotPreventOtherWrites() throws Exception {
        ask(true);

        mockMvc.perform(post("/api/programs/program-dashboard/milestones")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"actorAgentId": "%s", "title": "Still allowed", "position": 0}"""
                                .formatted(agentId)))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/programs/program-dashboard")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"actorAgentId": "%s", "status": "ACTIVE"}""".formatted(agentId)))
                .andExpect(status().isOk());
    }

    /** R7, R8. A blocking question shows up in the counts the overview renders. */
    @Test
    void anOpenBlockingQuestionIsCountedOnTheOverviewAndListedOnTheProgram() throws Exception {
        ask(true);

        mockMvc.perform(get("/api/programs"))
                .andExpect(jsonPath("$[?(@.slug == 'program-dashboard')].openClarificationCount").value(1))
                .andExpect(jsonPath("$[?(@.slug == 'program-dashboard')].blockingClarificationCount").value(1));

        mockMvc.perform(get("/api/programs/program-dashboard"))
                .andExpect(jsonPath("$.clarifications.length()").value(1))
                .andExpect(jsonPath("$.clarifications[0].blocking").value(true))
                .andExpect(jsonPath("$.clarifications[0].status").value("OPEN"));
    }

    private JsonNode ask(boolean blocking) throws Exception {
        String response = mockMvc.perform(post("/api/programs/program-dashboard/clarifications")
                        .contentType(APPLICATION_JSON).content(askBody(blocking)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return jsonMapper.readTree(response);
    }

    private String askBody(boolean blocking) {
        return """
                {"askedByAgentId": "%s",
                 "question": "Can any registered agent complete a milestone, or only the creating agent?",
                 "context": "R10 records completedByAgentId but never says who may set it.",
                 "blocking": %s,
                 "proposedOptions": [
                   {"label": "Any registered agent",
                    "rationale": "A second agent often picks up work mid-project."},
                   {"label": "Only the creating agent",
                    "rationale": "Prevents a stray script from marking work done."}]}"""
                .formatted(agentId, blocking);
    }
}
