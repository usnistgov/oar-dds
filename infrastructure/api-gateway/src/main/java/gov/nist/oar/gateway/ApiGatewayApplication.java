package gov.nist.oar.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Spring Cloud Gateway for API routing and load balancing.
 *
 * <p>This gateway serves as the single entry point for all client requests,
 * providing routing, load balancing, security, and resilience features.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Dynamic routing based on Eureka service discovery</li>
 *   <li>Load balancing across service instances</li>
 *   <li>Circuit breaker pattern for fault tolerance</li>
 *   <li>Rate limiting to prevent service overload</li>
 *   <li>Request/response filtering and transformation</li>
 *   <li>Centralized CORS and security policies</li>
 * </ul>
 *
 * <p><b>Routing Patterns:</b>
 * <ul>
 *   <li>/od/ds/** - Routes to dataset-access-service</li>
 *   <li>/aip/** - Routes to aip-access-service</li>
 *   <li>/cache/** - Routes to cache-management-service</li>
 *   <li>/bundle/** - Routes to bundle-download-plan-service and data-bundle-access-service</li>
 *   <li>/rpa/** - Routes to restricted-access-service</li>
 *   <li>/version/** - Routes to version-service</li>
 * </ul>
 *
 * <p><b>Access:</b>
 * <ul>
 *   <li>Gateway endpoint: http://localhost:8080</li>
 *   <li>Health: http://localhost:8080/actuator/health</li>
 *   <li>Routes: http://localhost:8080/actuator/gateway/routes</li>
 * </ul>
 *
 * <p><b>Circuit Breaker:</b>
 * Uses Resilience4j for automatic fallback when services are unavailable.
 *
 * <p><b>Service Discovery:</b>
 * Automatically discovers service instances from Eureka and performs
 * client-side load balancing.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
