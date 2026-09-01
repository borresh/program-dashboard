package no.borresh.programdashboard.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import no.borresh.programdashboard.support.TestApi;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.transaction.annotation.Transactional;

/** R1, R2, R3. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AgentApiTest {

    private final MockMvc mockMvc;
    private final JsonMapper objectMapper;
    private final TestApi api;

    AgentApiTest(MockMvc mockMvc, JsonMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.api = new TestApi(mockMvc, objectMapper);
    }

    @Test
    void registeringANewNameCreatesTheAgent() throws Exception {
        mockMvc.perform(post("/api/agents").contentType(APPLICATION_JSON).content("""
                        {"name": "opencode-implementer", "role": "IMPLEMENTER"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("opencode-implementer"))
                .andExpect(jsonPath("$.role").value("IMPLEMENTER"))
                .andExpect(jsonPath("$.registeredAt").isNotEmpty());
    }

    @Test
    void registeringAnExistingNameReturnsTheSameRecordWithoutCreatingASecond() throws Exception {
        UUID firstId = api.registerAgent("opencode-implementer", "IMPLEMENTER");

        // R1 is idempotent by name, so an agent can send this at the start of every
        // session. A different role in the second call must not overwrite the stored one.
        String second = mockMvc.perform(post("/api/agents").contentType(APPLICATION_JSON).content("""
                        {"name": "opencode-implementer", "role": "REVIEWER"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstId.toString()))
                .andExpect(jsonPath("$.role").value("IMPLEMENTER"))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'opencode-implementer')].id").value(firstId.toString()));

        JsonNode agent = objectMapper.readTree(second);
        assertThat(Instant.parse(agent.get("lastSeenAt").asText()))
                .isAfterOrEqualTo(Instant.parse(agent.get("registeredAt").asText()));
    }

    @Test
    void listingReturnsTheSeededHumanAgent() throws Exception {
        mockMvc.perform(get("/api/agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'harald')].role").value("HUMAN"));
    }

    @Test
    void anUnknownRoleNamesTheAllowedValues() throws Exception {
        mockMvc.perform(post("/api/agents").contentType(APPLICATION_JSON).content("""
                        {"name": "someone", "role": "ARCHITECT"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Malformed request body"))
                .andExpect(jsonPath("$.detail").value(allOf(
                        containsString("role"),
                        containsString("IMPLEMENTER"),
                        containsString("REVIEWER"),
                        containsString("HUMAN"))));
    }

    @Test
    void aBlankNameIsRejectedWithWhatWasExpected() throws Exception {
        mockMvc.perform(post("/api/agents").contentType(APPLICATION_JSON).content("""
                        {"name": "  ", "role": "IMPLEMENTER"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].expected").value(containsString("non-empty")));
    }

    /** R3. An unknown agent id on any write is a 404 that names the id. */
    @Test
    void anUnknownAgentIdOnAWriteIsRejectedByNumber() throws Exception {
        UUID unknown = UUID.fromString("11111111-1111-1111-1111-111111111111");

        mockMvc.perform(post("/api/programs").contentType(APPLICATION_JSON).content("""
                        {"slug": "ghost", "name": "Ghost", "initialPrompt": "# Ghost",
                         "createdByAgentId": "%s"}""".formatted(unknown)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Agent not found"))
                .andExpect(jsonPath("$.detail").value(containsString(unknown.toString())));
    }
}
