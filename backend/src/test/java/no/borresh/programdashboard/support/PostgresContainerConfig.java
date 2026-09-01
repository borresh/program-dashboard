package no.borresh.programdashboard.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * A real PostgreSQL for the {@code *IT} tests, on the same major version as production.
 *
 * <p>Imported only by tests that would not be meaningful on H2 — anything touching native
 * SQL or a column whose Java type is decided by the driver. The rest of the suite stays on
 * H2, where it needs no Docker and runs in milliseconds.
 *
 * <p>Running these from inside a dev container needs
 * {@code TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal}, because containers started
 * through a mounted Docker socket publish their ports on the host rather than on the dev
 * container's loopback. {@code up.sh} sets it automatically; see the README.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresContainerConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }
}
