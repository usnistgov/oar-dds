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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONArray;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import gov.nist.oar.distrib.BundlePlanServiceApplication;
import gov.nist.oar.distrib.service.DataPackagingService;

/**
 * Integration tests for BundleDownloadPlanController.
 * Tests the REST API endpoint for creating bundle download plans.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = BundlePlanServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "distrib.packaging.maxpackagesize=500000000",
    "distrib.packaging.maxfilecount=200",
    "distrib.packaging.allowedurls=",
    "distrib.packaging.allowedRedirects=1",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
public class BundleDownloadPlanControllerIntegrationTest {

    Logger logger = LoggerFactory.getLogger(BundleDownloadPlanControllerIntegrationTest.class);

    @LocalServerPort
    int port;

    TestRestTemplate websvc = new TestRestTemplate();

    @Autowired
    DataPackagingService packagingService;

    private String getBaseURL() {
        return "http://localhost:" + port;
    }

    @Test
    public void testContextLoads() {
        assertNotNull(packagingService, "DataPackagingService should be autowired");
    }

    @Test
    public void testGetBundlePlan() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create a valid bundle request with files
        String requestBody = """
            {
                "bundleName": "TestBundle",
                "includeFiles": [
                    {
                        "filePath": "data/file1.json",
                        "downloadUrl": "http://example.com/file1.json",
                        "fileSize": 1024
                    },
                    {
                        "filePath": "data/file2.json",
                        "downloadUrl": "http://example.com/file2.json",
                        "fileSize": 2048
                    }
                ],
                "bundleSize": 3072,
                "filesInBundle": 2
            }
            """;

        HttpEntity<String> req = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/_bundle_plan",
            HttpMethod.POST, req, String.class);

        logger.info("Bundle plan response status: " + resp.getStatusCode());
        logger.info("Bundle plan response: " + resp.getBody());

        assertEquals(HttpStatus.OK, resp.getStatusCode());

        String body = resp.getBody();
        assertNotNull(body);

        JSONObject plan = new JSONObject(new JSONTokener(body));
        // The response should contain bundle plan information
        assertTrue(plan.has("status") || plan.has("bundleNameFilePathUrl") || plan.has("notIncluded"),
                   "Response should contain bundle plan fields");
    }

    @Test
    public void testGetBundlePlanEmptyBundleName() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create request with empty bundle name (should fail validation)
        String requestBody = """
            {
                "bundleName": "",
                "includeFiles": [
                    {
                        "filePath": "data/file1.json",
                        "downloadUrl": "http://example.com/file1.json",
                        "fileSize": 1024
                    }
                ],
                "bundleSize": 1024,
                "filesInBundle": 1
            }
            """;

        HttpEntity<String> req = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/_bundle_plan",
            HttpMethod.POST, req, String.class);

        logger.info("Empty bundle name response status: " + resp.getStatusCode());
        logger.info("Empty bundle name response: " + resp.getBody());

        // Should return 400 Bad Request due to empty bundle name
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    public void testGetBundlePlanNullBundleName() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create request without bundle name
        String requestBody = """
            {
                "includeFiles": [
                    {
                        "filePath": "data/file1.json",
                        "downloadUrl": "http://example.com/file1.json",
                        "fileSize": 1024
                    }
                ],
                "bundleSize": 1024,
                "filesInBundle": 1
            }
            """;

        HttpEntity<String> req = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/_bundle_plan",
            HttpMethod.POST, req, String.class);

        logger.info("Null bundle name response status: " + resp.getStatusCode());
        logger.info("Null bundle name response: " + resp.getBody());

        // Should return 400 Bad Request due to missing bundle name
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    public void testGetBundlePlanEmptyBody() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Empty request body
        String requestBody = "{}";

        HttpEntity<String> req = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/_bundle_plan",
            HttpMethod.POST, req, String.class);

        logger.info("Empty body response status: " + resp.getStatusCode());
        logger.info("Empty body response: " + resp.getBody());

        // Should return 400 Bad Request
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    public void testGetBundlePlanLargeFileList() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create request with multiple files
        StringBuilder filesJson = new StringBuilder();
        filesJson.append("[");
        for (int i = 0; i < 10; i++) {
            if (i > 0) filesJson.append(",");
            filesJson.append(String.format("""
                {
                    "filePath": "data/file%d.json",
                    "downloadUrl": "http://example.com/file%d.json",
                    "fileSize": %d
                }
                """, i, i, 1024 * (i + 1)));
        }
        filesJson.append("]");

        String requestBody = String.format("""
            {
                "bundleName": "LargeBundle",
                "includeFiles": %s,
                "bundleSize": 56320,
                "filesInBundle": 10
            }
            """, filesJson.toString());

        HttpEntity<String> req = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/_bundle_plan",
            HttpMethod.POST, req, String.class);

        logger.info("Large file list response status: " + resp.getStatusCode());

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    @Test
    public void testGetMethodNotAllowed() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/_bundle_plan",
            HttpMethod.GET, req, String.class);

        // GET should not be allowed - only POST
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, resp.getStatusCode());
    }
}
