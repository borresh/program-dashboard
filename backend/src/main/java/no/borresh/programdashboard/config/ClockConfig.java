package no.borresh.programdashboard.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ClockConfig {

    /**
     * Injected wherever a timestamp is written, so time is a dependency a test can control
     * rather than a static call. UTC because every stored timestamp is UTC.
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
