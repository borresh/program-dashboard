package no.borresh.programdashboard.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Origins permitted to call {@code /api/**} from a browser.
 *
 * <p>Bound from {@code PROGRAM_DASHBOARD_CORS_ALLOWED_ORIGINS}. There is no default: an
 * application that cannot say who may call it should not start.
 */
@ConfigurationProperties(prefix = "program-dashboard.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException(
                    "program-dashboard.cors.allowed-origins must list at least one origin. "
                            + "Set PROGRAM_DASHBOARD_CORS_ALLOWED_ORIGINS, for example "
                            + "http://localhost:4200");
        }
        allowedOrigins = List.copyOf(allowedOrigins);
    }
}
