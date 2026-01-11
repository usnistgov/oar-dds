/**
 * This software was developed at the National Institute of Standards and Technology by employees of
 * the Federal Government in the course of their official duties. Pursuant to title 17 Section 105
 * of the United States Code this software is not subject to copyright protection and is in the
 * public domain. This is an experimental system. NIST assumes no responsibility whatsoever for its
 * use by other parties, and makes no guarantees, expressed or implied, about its quality,
 * reliability, or any other characteristic. We would appreciate acknowledgement if the software is
 * used. This software can be redistributed and/or modified freely provided that any derivative
 * works bear some notice that they are derived from it, and any modified versions bear some notice
 * that they have been modified.
 */
package gov.nist.oar.distrib.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import gov.nist.oar.common.utils.ServiceVersion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for aggregating version information from all microservices.
 * Provides both its own version and an aggregate view of all registered services.
 */
@RestController
@Tag(name = "Version Information API", description = "Get version information for the distribution system")
@RequestMapping(value = "/")
public class VersionController {

    private static final Logger logger = LoggerFactory.getLogger(VersionController.class);

    private static final ServiceVersion SERVICE_VERSION =
        new ServiceVersion("version-service");

    private static final String SYSTEM_NAME = "oar-data-distribution-system";
    private static final String SYSTEM_VERSION = "1.0.0-SNAPSHOT";

    @Autowired
    private DiscoveryClient discoveryClient;

    private final RestTemplate restTemplate = new RestTemplate();

    // Service endpoints for version info
    private static final Map<String, String> SERVICE_VERSION_PATHS = Map.of(
        "dataset-access-service", "/ds/",
        "cache-mgmt-service", "/cache/_version",
        "bundle-plan-service", "/ds/",
        "data-bundle-service", "/ds/",
        "aip-access-service", "/ds/_aip/",
        "restricted-access-service", "/ds/rpa/"
    );

    /**
     * Return version of this service.
     */
    @Operation(summary = "Return the version data for the version service",
               description = "This returns the name and version label for the version service itself")
    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ServiceVersion.VersionInfo getServiceVersion() {
        return SERVICE_VERSION.toVersionInfo();
    }

    /**
     * Return aggregate version information for all services in the system.
     */
    @Operation(summary = "Return aggregate version data for all services",
               description = "This returns version information for all registered microservices in the distribution system")
    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public SystemVersionInfo getAllVersions() {
        SystemVersionInfo info = new SystemVersionInfo();
        info.system = SYSTEM_NAME;
        info.systemVersion = SYSTEM_VERSION;
        info.services = new HashMap<>();

        for (Map.Entry<String, String> entry : SERVICE_VERSION_PATHS.entrySet()) {
            String serviceName = entry.getKey();
            String versionPath = entry.getValue();

            ServiceVersionStatus status = getServiceVersionStatus(serviceName, versionPath);
            info.services.put(serviceName, status);
        }

        return info;
    }

    /**
     * Get version status for a specific service.
     */
    private ServiceVersionStatus getServiceVersionStatus(String serviceName, String versionPath) {
        ServiceVersionStatus status = new ServiceVersionStatus();

        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
            if (instances == null || instances.isEmpty()) {
                status.status = "NOT_REGISTERED";
                status.version = null;
                return status;
            }

            ServiceInstance instance = instances.get(0);
            String url = instance.getUri().toString() + versionPath;

            try {
                ServiceVersion.VersionInfo versionInfo = restTemplate.getForObject(url, ServiceVersion.VersionInfo.class);
                if (versionInfo != null) {
                    status.version = versionInfo.version;
                    status.serviceName = versionInfo.serviceName;
                    status.status = "UP";
                } else {
                    status.status = "NO_VERSION_INFO";
                }
            } catch (Exception e) {
                logger.warn("Failed to get version from {}: {}", serviceName, e.getMessage());
                status.status = "ERROR";
                status.error = e.getMessage();
            }
        } catch (Exception e) {
            logger.warn("Failed to discover service {}: {}", serviceName, e.getMessage());
            status.status = "DISCOVERY_ERROR";
            status.error = e.getMessage();
        }

        return status;
    }

    /**
     * Response DTO for aggregate system version info.
     */
    public static class SystemVersionInfo {
        public String system;
        public String systemVersion;
        public Map<String, ServiceVersionStatus> services;
    }

    /**
     * DTO for individual service version status.
     */
    public static class ServiceVersionStatus {
        public String serviceName;
        public String version;
        public String status;
        public String error;
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorInfo handleStreamingError(RuntimeException ex, HttpServletRequest req) {
        logger.error("Unexpected failure during request: " + req.getRequestURI() +
                     "\n  " + ex.getMessage(), ex);
        return new ErrorInfo(req.getRequestURI(), 500, "Unexpected Server Error");
    }
}
