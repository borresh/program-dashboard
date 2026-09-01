package no.borresh.programdashboard;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exports the OpenAPI specification that the Angular client is generated from.
 *
 * <p>This is a test rather than a build plugin so that producing the contract needs
 * nothing but the test database: no port to bind, no running container, and the same
 * behaviour on Linux and WSL. It runs during the {@code test} phase, which is what puts
 * the file in place before the generator runs in {@code prepare-package}.
 *
 * <p>The specification is committed to neither the repository nor an image. If it is
 * missing, the frontend build fails loudly rather than falling back to a stale client.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiSpecExportTest {

    private final MockMvc mockMvc;

    OpenApiSpecExportTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void exportsTheSpecificationForTheFrontendClientGenerator() throws Exception {
        String specification = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(specification)
                .contains("\"/api/agents\"")
                .contains("\"/api/programs\"")
                .contains("\"/api/programs/{idOrSlug}\"")
                .contains("\"/api/milestones/{id}\"");

        Path output = Path.of(System.getProperty("openapi.spec.file", "target/openapi.json"));
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output, specification, UTF_8);
    }
}
