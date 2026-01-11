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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for ServiceVersion utility class.
 */
public class ServiceVersionTest {

    @Test
    public void testExplicitConstructor() {
        ServiceVersion sv = new ServiceVersion("test-service", "1.2.3");

        assertEquals("test-service", sv.getServiceName());
        assertEquals("1.2.3", sv.getVersion());
    }

    @Test
    public void testToVersionInfo() {
        ServiceVersion sv = new ServiceVersion("my-service", "2.0.0");
        ServiceVersion.VersionInfo info = sv.toVersionInfo();

        assertNotNull(info);
        assertEquals("my-service", info.serviceName);
        assertEquals("2.0.0", info.version);
    }

    @Test
    public void testVersionInfoDefaultConstructor() {
        ServiceVersion.VersionInfo info = new ServiceVersion.VersionInfo();
        // Should not throw, fields will be null
        assertNotNull(info);
    }

    @Test
    public void testVersionInfoParameterizedConstructor() {
        ServiceVersion.VersionInfo info = new ServiceVersion.VersionInfo("svc", "v1");
        assertEquals("svc", info.serviceName);
        assertEquals("v1", info.version);
    }

    @Test
    public void testDefaultConstructorWithoutVersionFile() {
        // When no VERSION file exists, should use default name and "not set" version
        ServiceVersion sv = new ServiceVersion("fallback-service");

        // Since shared-utils doesn't have a VERSION file, it should use defaults
        // The actual behavior depends on whether a VERSION file exists in the test classpath
        assertNotNull(sv.getServiceName());
        assertNotNull(sv.getVersion());
    }
}
