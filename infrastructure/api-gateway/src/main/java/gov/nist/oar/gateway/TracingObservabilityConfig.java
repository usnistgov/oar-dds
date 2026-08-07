package gov.nist.oar.gateway;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;

/**
 * Excludes Spring Boot Actuator requests (/actuator/**) from observations, so
 * Prometheus scrapes and Docker healthchecks do not create spans or HTTP request
 * metrics. Real request traces and metrics are unaffected; the actuator endpoints
 * still serve normally. (Reactive/WebFlux variant for the gateway.)
 */
@Configuration
public class TracingObservabilityConfig {

    @Bean
    ObservationPredicate excludeActuatorFromObservations() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext ctx && ctx.getCarrier() != null) {
                return !ctx.getCarrier().getURI().getPath().startsWith("/actuator");
            }
            return true;
        };
    }
}
