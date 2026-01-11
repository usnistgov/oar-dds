package gov.nist.oar.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Server for Service Discovery.
 *
 * <p>This server maintains a registry of all microservices in the OAR Data Distribution System
 * ecosystem. Services register themselves with Eureka on startup and send heartbeats to
 * remain registered.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Service registration and discovery</li>
 *   <li>Client-side load balancing support</li>
 *   <li>Self-preservation mode during network partitions</li>
 *   <li>Service health monitoring</li>
 * </ul>
 *
 * <p><b>Access:</b>
 * <ul>
 *   <li>Dashboard: http://localhost:8761</li>
 *   <li>Health: http://localhost:8761/actuator/health</li>
 * </ul>
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
