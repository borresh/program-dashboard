package no.borresh.programdashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Phase 1 gate. Proves that the single Flyway migration set applies to H2 in PostgreSQL
 * mode, that the seeded human agent is present under its fixed identifier, and that the
 * health endpoint is reachable.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationStartupTest {

    private static final UUID SEEDED_HUMAN_AGENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final JdbcClient jdbcClient;
    private final MockMvc mockMvc;

    ApplicationStartupTest(JdbcClient jdbcClient, MockMvc mockMvc) {
        this.jdbcClient = jdbcClient;
        this.mockMvc = mockMvc;
    }

    @Test
    void migrationCreatesEverySchemaObject() {
        var tables = jdbcClient
                .sql("SELECT LOWER(table_name) FROM information_schema.tables "
                        + "WHERE LOWER(table_schema) = 'public'")
                .query(String.class)
                .list();

        assertThat(tables).contains(
                "agent", "program", "milestone", "clarification", "clarification_option", "activity_entry");
    }

    @Test
    void migrationSeedsTheHumanAgentUnderItsFixedIdentifier() {
        var role = jdbcClient
                .sql("SELECT role FROM agent WHERE id = :id")
                .param("id", SEEDED_HUMAN_AGENT_ID)
                .query(String.class)
                .single();

        assertThat(role).isEqualTo("HUMAN");
    }

    @Test
    void seedingIsTheOnlyDataInTheDatabase() {
        var agentCount = jdbcClient.sql("SELECT COUNT(*) FROM agent").query(Long.class).single();

        assertThat(agentCount).isEqualTo(1L);
    }

    @Test
    void healthEndpointReportsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
