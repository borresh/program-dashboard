package no.borresh.programdashboard.program;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import tools.jackson.databind.json.JsonMapper;
import org.springframework.transaction.annotation.Transactional;

/** R4 to R9, and R12. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProgramApiTest {

    private final MockMvc mockMvc;
    private final TestApi api;

    private UUID agentId;

    ProgramApiTest(MockMvc mockMvc, JsonMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.api = new TestApi(mockMvc, objectMapper);
    }

    @BeforeEach
    void registerActingAgent() throws Exception {
        agentId = api.registerAgent("opencode-implementer", "IMPLEMENTER");
    }

    /** R4. */
    @Test
    void creatingAProgramStartsItAsAnIdeaAndReturnsItsLocation() throws Exception {
        mockMvc.perform(post("/api/programs").contentType(APPLICATION_JSON).content("""
                        {"slug": "program-dashboard", "name": "Program Dashboard",
                         "description": "Tracks what I build",
                         "initialPrompt": "# program-dashboard\\n\\nA browser dashboard.",
                         "createdByAgentId": "%s",
                         "milestones": [{"title": "Skeleton"}, {"title": "Agents and programs"}]}"""
                        .formatted(agentId)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/programs/program-dashboard"))
                .andExpect(jsonPath("$.status").value("IDEA"))
                .andExpect(jsonPath("$.createdBy.name").value("opencode-implementer"))
                .andExpect(jsonPath("$.totalMilestones").value(2))
                .andExpect(jsonPath("$.completedMilestones").value(0))
                .andExpect(jsonPath("$.progress").value(0.0))
                // Milestones supplied at creation keep the order they were given in.
                .andExpect(jsonPath("$.milestones[0].title").value("Skeleton"))
                .andExpect(jsonPath("$.milestones[0].position").value(0))
                .andExpect(jsonPath("$.milestones[1].position").value(1));
    }

    /** R5. */
    @Test
    void aDuplicateSlugNamesTheSlugAndTheProgramThatHasIt() throws Exception {
        api.createProgram("program-dashboard", agentId);

        mockMvc.perform(post("/api/programs").contentType(APPLICATION_JSON).content("""
                        {"slug": "program-dashboard", "name": "Something else",
                         "initialPrompt": "# other", "createdByAgentId": "%s"}""".formatted(agentId)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Slug already in use"))
                .andExpect(jsonPath("$.detail").value(containsString("program-dashboard")));
    }

    /** R6. */
    @Test
    void anInvalidSlugIsRejectedWithThePatternItMustMatch() throws Exception {
        mockMvc.perform(post("/api/programs").contentType(APPLICATION_JSON).content("""
                        {"slug": "Program Dashboard!", "name": "Program Dashboard",
                         "initialPrompt": "# x", "createdByAgentId": "%s"}""".formatted(agentId)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.errors[0].field").value("slug"))
                .andExpect(jsonPath("$.errors[0].expected")
                        .value(containsString("^[a-z0-9]+(-[a-z0-9]+)*$")))
                .andExpect(jsonPath("$.errors[0].rejectedValue").value("Program Dashboard!"));
    }

    /** R6. Every offending field is listed, not just the first one found. */
    @Test
    void severalInvalidFieldsAreAllReported() throws Exception {
        mockMvc.perform(post("/api/programs").contentType(APPLICATION_JSON).content("""
                        {"slug": "Bad Slug", "name": "", "initialPrompt": ""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(4))
                .andExpect(jsonPath("$.errors[0].field").value("createdByAgentId"))
                .andExpect(jsonPath("$.errors[1].field").value("initialPrompt"))
                .andExpect(jsonPath("$.errors[2].field").value("name"))
                .andExpect(jsonPath("$.errors[3].field").value("slug"));
    }

    /** R7. */
    @Test
    void theOverviewCarriesCountsForEveryProgram() throws Exception {
        api.createProgram("with-milestones", agentId, "One", "Two");
        api.createProgram("without-milestones", agentId);

        mockMvc.perform(get("/api/programs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug == 'with-milestones')].totalMilestones").value(2))
                .andExpect(jsonPath("$[?(@.slug == 'with-milestones')].openClarificationCount").value(0))
                .andExpect(jsonPath("$[?(@.slug == 'with-milestones')].blockingClarificationCount").value(0));
    }

    /** R12. A program with no milestones has no progress, which is not the same as none done. */
    @Test
    void aProgramWithoutMilestonesReportsNoProgressRatherThanZero() throws Exception {
        api.createProgram("without-milestones", agentId);

        mockMvc.perform(get("/api/programs/without-milestones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMilestones").value(0))
                .andExpect(jsonPath("$.progress").doesNotExist());
    }

    /** R8. */
    @Test
    void aProgramResolvesByBothItsSlugAndItsId() throws Exception {
        api.createProgram("program-dashboard", agentId);

        String id = mockMvc.perform(get("/api/programs/program-dashboard"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()
                .replaceAll("(?s).*?\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/programs/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("program-dashboard"));
    }

    /** R8. */
    @Test
    void anUnknownProgramIdentifierIsNamedInTheError() throws Exception {
        mockMvc.perform(get("/api/programs/no-such-program"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Program not found"))
                .andExpect(jsonPath("$.detail").value(containsString("no-such-program")));
    }

    /** R9. */
    @Test
    void patchingChangesOnlyTheFieldsThatWereSent() throws Exception {
        api.createProgram("program-dashboard", agentId);

        mockMvc.perform(patch("/api/programs/program-dashboard")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"actorAgentId": "%s", "status": "ACTIVE"}""".formatted(agentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.name").value("program-dashboard"));
    }

    /** R9. There is no delete endpoint; ABANDONED is how a program goes away. */
    @Test
    void aProgramCannotBeDeleted() throws Exception {
        api.createProgram("program-dashboard", agentId);

        mockMvc.perform(delete("/api/programs/program-dashboard"))
                .andExpect(status().isMethodNotAllowed());
    }
}
