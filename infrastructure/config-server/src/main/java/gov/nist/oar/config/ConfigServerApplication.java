package gov.nist.oar.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Spring Cloud Config Server for centralized configuration management.
 *
 * <p>This server provides centralized external configuration for all microservices.
 * Configuration is stored in a Git repository and served via REST endpoints.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Git-backed configuration storage</li>
 *   <li>Environment-specific configuration (local, dev, prod)</li>
 *   <li>Application-specific configuration</li>
 *   <li>Hot reload of configuration without service restart</li>
 *   <li>Encryption/decryption of sensitive properties</li>
 * </ul>
 *
 * <p><b>Configuration File Naming Convention:</b>
 * <ul>
 *   <li>application.yml - Shared by all services</li>
 *   <li>application-{profile}.yml - Environment-specific (local, dev, prod)</li>
 *   <li>{service-name}.yml - Service-specific configuration</li>
 *   <li>{service-name}-{profile}.yml - Service+environment specific</li>
 * </ul>
 *
 * <p><b>Access:</b>
 * <ul>
 *   <li>Config endpoint: http://localhost:8888/{application}/{profile}</li>
 *   <li>Health: http://localhost:8888/actuator/health</li>
 * </ul>
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
