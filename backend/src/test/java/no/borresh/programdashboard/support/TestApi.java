package no.borresh.programdashboard.support;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.UUID;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.test.web.servlet.ResultActions;

/**
 * The two calls every API test needs before it can test anything interesting: an agent to
 * act as, and a program to act on. Extracted because three test classes needed the same
 * setup, not in anticipation of a fourth.
 */
public final class TestApi {

    private final MockMvc mockMvc;
    private final JsonMapper objectMapper;

    public TestApi(MockMvc mockMvc, JsonMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    public UUID registerAgent(String name, String role) throws Exception {
        String body = """
                {"name": "%s", "role": "%s"}""".formatted(name, role);

        return UUID.fromString(idOf(mockMvc.perform(post("/api/agents")
                .contentType(APPLICATION_JSON)
                .content(body))));
    }

    public String createProgram(String slug, UUID createdByAgentId, String... milestoneTitles) throws Exception {
        StringBuilder milestones = new StringBuilder();
        for (int index = 0; index < milestoneTitles.length; index++) {
            milestones.append(index == 0 ? "" : ", ")
                    .append("""
                            {"title": "%s"}""".formatted(milestoneTitles[index]));
        }

        String body = """
                {"slug": "%s", "name": "%s", "initialPrompt": "# %s",
                 "createdByAgentId": "%s", "milestones": [%s]}"""
                .formatted(slug, slug, slug, createdByAgentId, milestones);

        mockMvc.perform(post("/api/programs").contentType(APPLICATION_JSON).content(body));
        return slug;
    }

    public JsonNode parse(String json) throws Exception {
        return objectMapper.readTree(json);
    }

    private String idOf(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString())
                .get("id")
                .asText();
    }
}
