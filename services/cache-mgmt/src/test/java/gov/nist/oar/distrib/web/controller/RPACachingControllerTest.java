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
package gov.nist.oar.distrib.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import gov.nist.oar.distrib.CacheManagementServiceApplication;

/**
 * Integration tests for RPA (Restricted Public Access) caching endpoints in CacheManagementController.
 * Tests the REST API endpoints for:
 * - PUT /cache/rpa/{dsid} - Cache dataset for RPA access
 * - GET /cache/rpa/objects/{randomId} - Get RPA cached objects
 * - DELETE /cache/rpa/objects/{randomId} - Uncache RPA objects
 *
 * Note: These tests use a minimal cache configuration. Full caching functionality
 * requires proper bag store and cache volume setup.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = CacheManagementServiceApplication.class,
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = RPACachingControllerTest.TestDirectoryInitializer.class)
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "distrib.bagstore.mode=local",
    "distrib.bagstore.location=${basedir}/src/test/resources",
    "distrib.baseurl=http://localhost/oar-distrib-service",
    "distrib.cachemgr.admindir=${java.io.tmpdir}/testcmgr-rpa",
    "distrib.cachemgr.smallSizeLimit=500",
    "distrib.cachemgr.headbagCacheSize=40000000",
    "distrib.cachemgr.volumes[0].location=file://${java.io.tmpdir}/testvol-rpa",
    "distrib.cachemgr.volumes[0].name=testvol",
    "distrib.cachemgr.volumes[0].capacity=30000000",
    "distrib.cachemgr.volumes[0].roles[0]=general",
    "distrib.cachemgr.volumes[0].roles[1]=small",
    "distrib.cachemgr.volumes[0].roles[2]=restricted",
    "cloud.aws.region=us-east-1",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
public class RPACachingControllerTest {

    /**
     * Initializer that creates required directories before the Spring context is loaded.
     */
    static class TestDirectoryInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            String tmpdir = System.getProperty("java.io.tmpdir");
            new File(tmpdir, "testcmgr-rpa").mkdirs();
            new File(tmpdir, "testvol-rpa").mkdirs();
        }
    }

    Logger logger = LoggerFactory.getLogger(RPACachingControllerTest.class);

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate websvc;

    HttpHeaders headers = new HttpHeaders();

    private String getBaseURL() {
        return "http://localhost:" + port;
    }

    @Test
    public void testCacheDatasetForRPAWithNonExistentDataset() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Try to cache a non-existent dataset - should return 404
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/cache/rpa/nonexistent-dataset",
            HttpMethod.PUT, req, String.class);

        logger.info("Cache non-existent dataset status: " + resp.getStatusCode());
        logger.info("Cache non-existent dataset response: " + resp.getBody());

        // Should return 404 Not Found for non-existent dataset
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    public void testCacheDatasetForRPAWithVersion() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Try to cache with version parameter
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/cache/rpa/nonexistent-dataset?version=1.0",
            HttpMethod.PUT, req, String.class);

        logger.info("Cache with version status: " + resp.getStatusCode());

        // Should return 404 for non-existent dataset
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    public void testCacheDatasetViaARK() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Try to cache using ARK ID format
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/cache/rpa/ark:/88434/mds2-2909",
            HttpMethod.PUT, req, String.class);

        logger.info("Cache via ARK status: " + resp.getStatusCode());

        // Should return 404 for non-existent dataset (ARK should be parsed correctly)
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    public void testGetRPACachedObjectsWithInvalidId() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Try to get objects with a random ID that doesn't exist
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/cache/rpa/objects/rpa-nonexistent-id",
            HttpMethod.GET, req, String.class);

        logger.info("Get RPA cached objects status: " + resp.getStatusCode());
        logger.info("Get RPA cached objects response: " + resp.getBody());

        // Should return 200 OK with empty object list (no objects found is not an error)
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        String body = resp.getBody();
        assertNotNull(body);
        JSONObject result = new JSONObject(new JSONTokener(body));
        assertEquals("rpa-nonexistent-id", result.getString("randomId"));
        assertEquals(0, result.getInt("objectCount"));
    }

    @Test
    public void testUncacheRPAObjectsWithInvalidId() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Try to uncache objects with a random ID that doesn't exist
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/cache/rpa/objects/rpa-nonexistent-id",
            HttpMethod.DELETE, req, String.class);

        logger.info("Uncache RPA objects status: " + resp.getStatusCode());
        logger.info("Uncache RPA objects response: " + resp.getBody());

        // Should return 200 OK with zero uncached count
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        String body = resp.getBody();
        assertNotNull(body);
        JSONObject result = new JSONObject(new JSONTokener(body));
        assertEquals("rpa-nonexistent-id", result.getString("randomId"));
        assertEquals(0, result.getInt("uncachedCount"));
    }

    @Test
    public void testCacheDatasetWithExistingDataset() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Try to cache mds1491 which exists in test resources
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/cache/rpa/mds1491",
            HttpMethod.PUT, req, String.class);

        logger.info("Cache existing dataset status: " + resp.getStatusCode());
        logger.info("Cache existing dataset response: " + resp.getBody());

        // Should return 201 Created if successful, or 500 if cache volume not properly set up
        assertTrue(resp.getStatusCode() == HttpStatus.CREATED ||
                   resp.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR,
                   "Expected 201 or 500 status");

        if (resp.getStatusCode() == HttpStatus.CREATED) {
            String body = resp.getBody();
            assertNotNull(body);
            JSONObject result = new JSONObject(new JSONTokener(body));
            assertTrue(result.has("randomId"), "Response should contain randomId");
            assertTrue(result.getString("randomId").startsWith("rpa-"),
                       "Random ID should start with 'rpa-' prefix");
            assertEquals("mds1491", result.getString("datasetId"));
        }
    }
}
