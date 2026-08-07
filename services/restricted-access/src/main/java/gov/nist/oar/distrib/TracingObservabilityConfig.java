package gov.nist.oar.distrib;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Excludes Spring Boot Actuator requests (/actuator/**) from observations, so
 * Prometheus scrapes and Docker healthchecks do not create spans (or http
 * request metrics). Real request traces and metrics are unaffected; the
 * actuator endpoints still serve normally.
 */
@Configuration
public class TracingObservabilityConfig {

    @Bean
    ObservationPredicate excludeActuatorFromObservations() {
        return (name, context) -> {
            if (context instanceof org.springframework.http.server.observation.ServerRequestObservationContext servlet
                    && servlet.getCarrier() != null) {
                return !servlet.getCarrier().getRequestURI().startsWith("/actuator");
            }
            if (context instanceof org.springframework.http.server.reactive.observation.ServerRequestObservationContext reactive
                    && reactive.getCarrier() != null) {
                return !reactive.getCarrier().getURI().getPath().startsWith("/actuator");
            }
            return true;
        };
    }
}
