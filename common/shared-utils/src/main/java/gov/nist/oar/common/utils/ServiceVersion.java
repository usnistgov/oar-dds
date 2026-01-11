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
package gov.nist.oar.common.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Utility class for reading service version information from the VERSION resource file.
 * Each service should have a VERSION file in src/main/resources containing:
 * service-name version
 *
 * Example: oar-dds-dataset-access-service 1.0.0-SNAPSHOT
 */
public class ServiceVersion {

    private final String serviceName;
    private final String version;

    /**
     * Creates a ServiceVersion by reading from the VERSION resource file.
     *
     * @param defaultServiceName the default service name if VERSION file is not found
     */
    public ServiceVersion(String defaultServiceName) {
        String name = defaultServiceName;
        String ver = "not set";

        try (InputStream verf = ServiceVersion.class.getResourceAsStream("/VERSION")) {
            if (verf != null) {
                BufferedReader vrdr = new BufferedReader(new InputStreamReader(verf));
                String line = vrdr.readLine();
                if (line != null) {
                    String[] parts = line.split("\\s+");
                    name = parts[0];
                    ver = (parts.length > 1) ? parts[1] : "missing";
                }
            }
        } catch (Exception ex) {
            ver = "unknown";
        }

        this.serviceName = name;
        this.version = ver;
    }

    /**
     * Creates a ServiceVersion with explicit values.
     *
     * @param serviceName the service name
     * @param version the version string
     */
    public ServiceVersion(String serviceName, String version) {
        this.serviceName = serviceName;
        this.version = version;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getVersion() {
        return version;
    }

    /**
     * Response DTO for version endpoints.
     */
    public static class VersionInfo {
        public String serviceName;
        public String version;

        public VersionInfo() {}

        public VersionInfo(String name, String ver) {
            this.serviceName = name;
            this.version = ver;
        }
    }

    /**
     * Get version info as a DTO suitable for JSON serialization.
     */
    public VersionInfo toVersionInfo() {
        return new VersionInfo(serviceName, version);
    }
}
