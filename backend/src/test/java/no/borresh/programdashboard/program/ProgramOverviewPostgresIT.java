package no.borresh.programdashboard.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import no.borresh.programdashboard.support.PostgresContainerConfig;
import no.borresh.programdashboard.support.TestApi;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * The overview, against a real PostgreSQL.
 *
 * <p>This test exists because of a specific bug. {@code findAllCounts()} is native SQL, and
 * the two engines disagree about Java types: H2 returns a {@code timestamp with time zone}
 * as {@code OffsetDateTime} and its {@code uuid} columns as {@code byte[]}, while
 * PostgreSQL returns {@code Instant} and a real UUID. An earlier version of that query
 * selected the timestamp directly, passed every H2 test, and returned 500 in production.
 *
 * <p>So this is not a duplicate of {@link ProgramApiTest}. It is the only test that would
 * catch that class of failure, and it is the reason the query now selects nothing but an
 * identifier and four counts.
 */
@SpringBootTest(properties = "program-dashboard.cors.allowed-origins=http://localhost:4200")
@AutoConfigureMockMvc
@Import(PostgresContainerConfig.class)
class ProgramOverviewPostgresIT {

    private final MockMvc mockMvc;
    private final TestApi api;

    ProgramOverviewPostgresIT(MockMvc mockMvc, JsonMapper jsonMapper) {
        this.mockMvc = mockMvc;
        this.api = new TestApi(mockMvc, jsonMapper);
    }

    @Test
    void theOverviewSurvivesTheRoundTripThroughPostgresTypes() throws Exception {
        UUID agentId = api.registerAgent("postgres-probe", "IMPLEMENTER");
        api.createProgram("counts-on-postgres", agentId, "One", "Two");

        String milestoneId = mockMvc.perform(get("/api/programs/counts-on-postgres"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()
                .replaceAll("(?s).*\"milestones\":\\[\\{\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(patch("/api/milestones/" + milestoneId)
                .contentType(APPLICATION_JSON)
                .content("""
                        {"actorAgentId": "%s", "complete": true}""".formatted(agentId)))
                .andExpect(status().isOk());

        // Every field here crosses the native-query boundary or is derived from it.
        mockMvc.perform(get("/api/programs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug == 'counts-on-postgres')].id").isNotEmpty())
                .andExpect(jsonPath("$[?(@.slug == 'counts-on-postgres')].updatedAt").isNotEmpty())
                .andExpect(jsonPath("$[?(@.slug == 'counts-on-postgres')].totalMilestones").value(2))
                .andExpect(jsonPath("$[?(@.slug == 'counts-on-postgres')].completedMilestones").value(1))
                .andExpect(jsonPath("$[?(@.slug == 'counts-on-postgres')].progress").value(0.5))
                .andExpect(jsonPath("$[?(@.slug == 'counts-on-postgres')].openClarificationCount").value(0))
                .andExpect(jsonPath("$[?(@.slug == 'counts-on-postgres')].blockingClarificationCount").value(0));
    }

    @Test
    void theMigrationAppliesToRealPostgresAndSeedsTheHumanAgent() throws Exception {
        String agents = mockMvc.perform(get("/api/agents"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(agents).contains("harald").contains("HUMAN");
    }

    @Test
    void aProgramWithoutMilestonesReportsNoProgressOnPostgresToo() throws Exception {
        UUID agentId = api.registerAgent("postgres-probe-empty", "IMPLEMENTER");
        api.createProgram("empty-on-postgres", agentId);

        mockMvc.perform(get("/api/programs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug == 'empty-on-postgres')].totalMilestones").value(0))
                // Absent, not zero: R12's "unknown". A 0 here would make a program nobody
                // has broken down yet look like one nobody has started.
                .andExpect(jsonPath("$[?(@.slug == 'empty-on-postgres')].progress").isEmpty());
    }
}
