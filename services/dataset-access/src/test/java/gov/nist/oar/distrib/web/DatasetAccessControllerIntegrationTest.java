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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import gov.nist.oar.distrib.DatasetAccessServiceApplication;
import gov.nist.oar.distrib.service.NerdmDownloadService;
import gov.nist.oar.distrib.service.PreservationBagService;
import gov.nist.oar.common.cache.client.CacheManagerClient;

/**
 * Integration tests for DatasetAccessController.
 * Tests the REST API endpoints for dataset and file access.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {DatasetAccessServiceApplication.class, DatasetAccessControllerIntegrationTest.TestConfig.class},
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "distrib.bagstore.mode=local",
    "distrib.bagstore.location=${basedir}/src/test/resources",
    "distrib.baseurl=http://localhost/oar-distrib-service",
    "distrib.nerdm.baseurl=http://localhost:9999/od/id",
    "cloud.aws.region=us-east-1",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
public class DatasetAccessControllerIntegrationTest {

    Logger logger = LoggerFactory.getLogger(DatasetAccessControllerIntegrationTest.class);

    @LocalServerPort
    int port;

    TestRestTemplate websvc = new TestRestTemplate();
    HttpHeaders headers = new HttpHeaders();

    @Autowired
    PreservationBagService preservationBagService;

    @MockBean
    private NerdmDownloadService nerdmService;

    private JsonNode mockNerdm(String dsid) throws IOException {
        String json = Files.readString(Paths.get("src/test/resources/datasets/" + dsid + "/nerdm.json"));
        return new ObjectMapper().readTree(json);
    }

    /**
     * Test configuration that provides mock implementations for external dependencies.
     */
    @TestConfiguration
    static class TestConfig {

        /**
         * Mock CacheManagerClient that returns empty/null for all cache lookups.
         * This causes the service to fall back to direct bag access.
         */
        @Bean
        @Primary
        public CacheManagerClient testCacheManagerClient() {
            return new CacheManagerClient() {
                @Override
                public java.util.Optional<gov.nist.oar.common.cache.dto.CacheObjectInfo> getObjectInfo(String datasetId, String filepath) {
                    return java.util.Optional.empty();
                }

                @Override
                public boolean isCached(String datasetId, String filepath) {
                    return false;
                }

                @Override
                public gov.nist.oar.common.cache.dto.CacheObjectSummary getDatasetSummary(String datasetId) {
                    return null;
                }

                @Override
                public void queueForCaching(String datasetId, String filepath, boolean recache) {
                    // No-op
                }

                @Override
                public void removeFromCache(String datasetId, String filepath) {
                    // No-op
                }

                @Override
                public boolean isAvailable() {
                    return false;
                }

                @Override
                public java.util.Map<String, Object> getResourceMetadata(String datasetId) {
                    return java.util.Collections.emptyMap();
                }

                @Override
                public java.util.Map<String, Object> getComponentMetadata(String datasetId, String filepath) {
                    return java.util.Collections.emptyMap();
                }

                @Override
                public java.util.Map<String, Object> cacheDatasetForRPA(String datasetId, String version) {
                    return java.util.Collections.emptyMap();
                }

                @Override
                public java.util.Map<String, Object> getRPACachedObjects(String randomId) {
                    return java.util.Collections.emptyMap();
                }

                @Override
                public boolean uncacheRPA(String randomId) {
                    return false;
                }
            };
        }
    }

    private String getBaseURL() {
        return "http://localhost:" + port;
    }

    @Test
    public void testContextLoads() {
        assertNotNull(preservationBagService, "PreservationBagService should be autowired");
    }

    @Test
    public void testDescribeAIPs() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Test getting AIP descriptions for mds1491
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/mds1491/_aip",
            HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());

        String body = resp.getBody();
        assertNotNull(body);
        logger.info("AIP response: " + body);

        JSONArray aips = new JSONArray(new JSONTokener(body));
        assertTrue(aips.length() > 0, "Should have at least one AIP");

        // Verify structure of first AIP
        JSONObject firstAip = aips.getJSONObject(0);
        assertTrue(firstAip.has("name"), "AIP should have a name");
        assertTrue(firstAip.getString("name").contains("mds1491"), "AIP name should contain dataset ID");
    }

    @Test
    public void testDescribeAIPsNotFound() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Test getting AIP descriptions for non-existent dataset
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/nonexistent/_aip",
            HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    public void testListAIPVersions() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Test getting AIP versions for mds1491
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/mds1491/_aip/_v",
            HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());

        String body = resp.getBody();
        assertNotNull(body);
        logger.info("Versions response: " + body);

        JSONArray versions = new JSONArray(new JSONTokener(body));
        assertTrue(versions.length() > 0, "Should have at least one version");
    }

    @Test
    public void testDescribeAIPsForVersion() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // First get the versions
        ResponseEntity<String> versionsResp = websvc.exchange(
            getBaseURL() + "/ds/mds1491/_aip/_v",
            HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, versionsResp.getStatusCode());

        JSONArray versions = new JSONArray(new JSONTokener(versionsResp.getBody()));
        if (versions.length() > 0) {
            String version = versions.getString(0);

            // Test getting AIPs for that version
            ResponseEntity<String> resp = websvc.exchange(
                getBaseURL() + "/ds/mds1491/_aip/_v/" + version,
                HttpMethod.GET, req, String.class);

            assertEquals(HttpStatus.OK, resp.getStatusCode());

            String body = resp.getBody();
            assertNotNull(body);
            logger.info("Version AIPs response: " + body);
        }
    }

    @Test
    public void testDescribeAIPsForLatestVersion() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Test getting AIPs for "latest" version
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/mds1491/_aip/_v/latest",
            HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());

        String body = resp.getBody();
        assertNotNull(body);
        logger.info("Latest version AIPs response: " + body);

        JSONArray aips = new JSONArray(new JSONTokener(body));
        assertTrue(aips.length() > 0, "Should have at least one AIP for latest version");
    }

    @Test
    public void testDescribeLatestHeadAIP() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Test getting head bag description
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/mds1491/_aip/_head",
            HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());

        String body = resp.getBody();
        assertNotNull(body);
        logger.info("Head AIP response: " + body);

        JSONObject headBag = new JSONObject(new JSONTokener(body));
        assertTrue(headBag.has("name"), "Head bag should have a name");
    }

    @Test
    public void testNonExistentDatasetReturns404() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Test with non-existent dataset ID (should return 404)
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/nonexistent-dataset/_aip",
            HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());

        String body = resp.getBody();
        assertNotNull(body);
        // The response should contain error information
        JSONObject error = new JSONObject(new JSONTokener(body));
        assertEquals(404, error.getInt("status"));
    }

    // === Security Tests ===

    @Test
    public void testDescribeAIPsEvil() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        // URL-encoded <script>goober</script> - should not be reflected back in response
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/%3Cscript%3Egoober%3C%2Fscript%3E/_aip",
            HttpMethod.GET, req, String.class);
        // Microservices version returns 404 (not found) for invalid IDs
        assertTrue(resp.getStatusCode() == HttpStatus.BAD_REQUEST ||
                   resp.getStatusCode() == HttpStatus.NOT_FOUND);
        assertFalse(resp.getBody().contains("script>"));
    }

    @Test
    public void testDescribeAIPsEvil2() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        // URL-encoded <script>goober<script> - should not be reflected back in response
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/%3Cscript%3Egoober%3Cscript%3E/_aip",
            HttpMethod.GET, req, String.class);
        // Microservices version returns 404 (not found) for invalid IDs
        assertTrue(resp.getStatusCode() == HttpStatus.BAD_REQUEST ||
                   resp.getStatusCode() == HttpStatus.NOT_FOUND);
        assertFalse(resp.getBody().contains("script>"));
    }

    // === Input Validation Tests ===

    @Test
    public void testBadDatasetIDPattern() {
        assertTrue(DatasetAccessController.baddsid.matcher("goober gurn").find());
        assertTrue(DatasetAccessController.baddsid.matcher("goober\tgurn").find());
        assertFalse(DatasetAccessController.baddsid.matcher("goober").find());
    }

    @Test
    public void testBadFilePathPattern() {
        assertTrue(DatasetAccessController.badpath.matcher(".goobergurn").find());
        assertTrue(DatasetAccessController.badpath.matcher("goober/../../../gurn").find());
        assertFalse(DatasetAccessController.badpath.matcher("goober..gurn").find());
    }

    @Test
    public void testDownloadFileBadInp() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/goober/trial1.json",
            HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());

        req = new HttpEntity<>(null, headers);
        resp = websvc.exchange(
            getBaseURL() + "/ds/mds1491/goober/trial1.json",
            HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    public void testDownloadFileMalInp() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        // Space in dataset ID should be rejected
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/goob er/trial1.json",
            HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());

        req = new HttpEntity<>(null, headers);
        // Path starting with dot should be rejected
        resp = websvc.exchange(
            getBaseURL() + "/ds/mds1491/.trial3a.json",
            HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    public void testDownloadFileInfo() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/mds1491/trial1.json",
            HttpMethod.HEAD, req, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNull(resp.getBody());
    }

    @Test
    public void testDownloadFileInfoViaARK() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/ark:/8888/mds1491/trial1.json",
            HttpMethod.HEAD, req, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNull(resp.getBody());
    }

    @Test
    public void testDownloadFileInfoViaBadARK() {
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<String> req = new HttpEntity<>(null, jsonHeaders);

        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/ark:/mds1491/goob/trial1.json",
            HttpMethod.HEAD, req, String.class);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertNull(resp.getBody());
    }

    @Test
    public void testDownloadFile() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/mds1491/trial1.json",
            HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getHeaders().getFirst("Content-Type").startsWith("application/json"));
        assertEquals(69, resp.getBody().length());
    }

    @Test
    public void testDownloadFileViaARK() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/ark:/1212/mds1491/trial1.json",
            HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getHeaders().getFirst("Content-Type").startsWith("application/json"));
        assertEquals(69, resp.getBody().length());
    }

    @Test
    public void testDownloadFileFromVersion() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/mds1491/_v/0/trial1.json",
            HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getHeaders().getFirst("Content-Type").startsWith("application/json"));
        assertEquals(69, resp.getBody().length());

        req = new HttpEntity<>(null, headers);
        resp = websvc.exchange(
            getBaseURL() + "/ds/mds1491/_v/1.1.0/trial1.json",
            HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getHeaders().getFirst("Content-Type").startsWith("application/json"));
        assertEquals(69, resp.getBody().length());
    }

    @Test
    public void testDescribeHeadAIPForVersionBadInp() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/goober/_aip/_head",
            HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    // === Rclone Index Tests ===

    @Test
    public void testRcloneIndexOrInvalidDatasetID() throws Exception {
        when(nerdmService.fetchNerdm("mds1491")).thenReturn(mockNerdm("mds1491"));

        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Case 1: Invalid dataset (no trailing slash - should be file request)
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/mds1491.json",
            HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());

        // Case 2: Valid dataset directory request (rclone-style index)
        resp = websvc.exchange(
            getBaseURL() + "/ds/mds1491/",
            HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getHeaders().getFirst("Content-Type").startsWith("text/html"));
    }

    @Test
    public void testRcloneIndexRootFolder() throws Exception {
        when(nerdmService.fetchNerdm("mds1491")).thenReturn(mockNerdm("mds1491"));

        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/mds1491/",
            HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getHeaders().getFirst("Content-Type").contains("text/html"));
        assertTrue(resp.getBody().contains("<title>"));
        assertTrue(resp.getBody().contains("trial1.json"));
    }

    @Test
    public void testRcloneIndexSubfolder() throws Exception {
        when(nerdmService.fetchNerdm("mds1491")).thenReturn(mockNerdm("mds1491"));

        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/mds1491/subfolder/",
            HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getHeaders().getFirst("Content-Type").startsWith("text/html"));
    }

    @Test
    public void testRcloneIndexRestrictedComponentsAreSkipped() throws Exception {
        when(nerdmService.fetchNerdm("mds1491")).thenReturn(mockNerdm("mds1491"));

        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/mds1491/",
            HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getHeaders().getFirst("Content-Type").startsWith("text/html"));
        // Restricted components should not appear in the listing
        assertFalse(resp.getBody().contains("restricted-file.json"));
    }

    @Test
    public void testRcloneIndexEmptyFolder() throws Exception {
        when(nerdmService.fetchNerdm("emptydataset")).thenReturn(mockNerdm("emptydataset"));

        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/emptydataset/",
            HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getHeaders().getFirst("Content-Type").startsWith("text/html"));
    }

    @Test
    public void testRcloneIndexEmptyFolderMissingNerdm() throws Exception {
        when(nerdmService.fetchNerdm("emptydataset")).thenReturn(null);

        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/emptydataset/",
            HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    // === Additional tests from monolith ===

    @Test
    public void testDescribeAIPsBadID() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/goober/_aip",
            HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        JSONObject error = new JSONObject(new JSONTokener(resp.getBody()));
        assertEquals(404, error.getInt("status"));
        assertTrue(error.getString("message").contains("not found"));
    }

    @Test
    public void testListAIPVersionsBadID() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/goober/_aip/_v",
            HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        JSONObject error = new JSONObject(new JSONTokener(resp.getBody()));
        assertEquals(404, error.getInt("status"));
    }

    @Test
    public void testDescribeHeadAIPForVersion() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Test getting head bag for version 0
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/mds1491/_aip/_v/0/_head",
            HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        JSONObject headBag = new JSONObject(new JSONTokener(resp.getBody()));
        assertTrue(headBag.has("name"));
        assertTrue(headBag.getString("name").contains("mds1491"));

        // Test getting head bag for version 1.1.0
        resp = websvc.exchange(
            getBaseURL() + "/ds/mds1491/_aip/_v/1.1.0/_head",
            HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        headBag = new JSONObject(new JSONTokener(resp.getBody()));
        assertTrue(headBag.has("name"));
        assertTrue(headBag.getString("name").contains("mds1491"));
    }

    @Test
    public void testDescribeAIPsForVersionBadIn() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Bad dataset ID
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/goober/_aip/_v/0",
            HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());

        // Bad version number
        resp = websvc.exchange(
            getBaseURL() + "/ds/mds1491/_aip/_v/12.32",
            HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    public void testDdescribeLatestHeadAIP() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/mds1491/_aip/_head",
            HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        JSONObject headBag = new JSONObject(new JSONTokener(resp.getBody()));
        assertTrue(headBag.has("name"));
        assertTrue(headBag.getString("name").contains("mds1491"));
        assertTrue(resp.getHeaders().getFirst("Content-Type").startsWith("application/json"));
    }
}
