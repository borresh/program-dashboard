package no.borresh.programdashboard.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    /**
     * The backend owns the API contract. This specification is exported during the build
     * and the Angular client is generated from it, so nothing in the frontend restates a
     * backend type by hand.
     */
    @Bean
    OpenAPI programDashboardOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Program Dashboard API")
                .version("1.0.0")
                .description("Tracks programs built with AI agents. Both the browser and the agents "
                        + "use this API; there is no separate backend-for-frontend."));
    }
}
