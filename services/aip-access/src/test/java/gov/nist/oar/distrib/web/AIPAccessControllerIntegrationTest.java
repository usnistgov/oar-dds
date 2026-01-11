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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import gov.nist.oar.distrib.AipAccessServiceApplication;
import gov.nist.oar.distrib.service.PreservationBagService;

/**
 * Integration tests for AIPAccessController.
 * Tests the REST API endpoints for AIP (bag) file access.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AipAccessServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "distrib.bagstore.mode=local",
    "distrib.bagstore.location=${basedir}/src/test/resources",
    "distrib.baseurl=http://localhost/oar-distrib-service",
    "cloud.aws.region=us-east-1",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
public class AIPAccessControllerIntegrationTest {

    Logger logger = LoggerFactory.getLogger(AIPAccessControllerIntegrationTest.class);

    @LocalServerPort
    int port;

    TestRestTemplate websvc = new TestRestTemplate();
    HttpHeaders headers = new HttpHeaders();

    @Autowired
    PreservationBagService preservationBagService;

    private String getBaseURL() {
        return "http://localhost:" + port;
    }

    @Test
    public void testContextLoads() {
        assertNotNull(preservationBagService, "PreservationBagService should be autowired");
    }

    @Test
    public void testDownloadAIP() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Download an existing AIP bag
        ResponseEntity<byte[]> resp = websvc.exchange(
            getBaseURL() + "/ds/_aip/mds1491.mbag0_2-0.zip",
            HttpMethod.GET, req, byte[].class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().length > 0, "Response body should not be empty");

        // Check Content-Type header
        String contentType = resp.getHeaders().getFirst("Content-Type");
        assertNotNull(contentType);
        assertTrue(contentType.contains("application/") || contentType.contains("zip"),
                   "Content-Type should be application/zip or similar");

        // Check Content-Length header
        String contentLength = resp.getHeaders().getFirst("Content-Length");
        assertNotNull(contentLength);
        assertTrue(Long.parseLong(contentLength) > 0, "Content-Length should be > 0");

        logger.info("Downloaded AIP with size: " + resp.getBody().length);
    }

    @Test
    public void testDownloadAIPNotFound() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Try to download a non-existent bag
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/_aip/nonexistent.mbag0_2-0.zip",
            HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());

        String body = resp.getBody();
        assertNotNull(body);
        logger.info("Not found response: " + body);

        JSONObject error = new JSONObject(new JSONTokener(body));
        assertEquals(404, error.getInt("status"));
    }

    @Test
    public void testDownloadAIPHead() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // HEAD request for AIP info
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/_aip/mds1491.mbag0_2-0.zip",
            HttpMethod.HEAD, req, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());

        // Check Content-Length header
        String contentLength = resp.getHeaders().getFirst("Content-Length");
        assertNotNull(contentLength, "Content-Length header should be present");
        assertTrue(Long.parseLong(contentLength) > 0, "Content-Length should be > 0");

        // Check Content-Type header
        String contentType = resp.getHeaders().getFirst("Content-Type");
        assertNotNull(contentType, "Content-Type header should be present");

        logger.info("HEAD Content-Length: " + contentLength);
    }

    @Test
    public void testDescribeAIP() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Get AIP description/info
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/_aip/mds1491.mbag0_2-0.zip/_info",
            HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());

        String body = resp.getBody();
        assertNotNull(body);
        logger.info("AIP info response: " + body);

        JSONObject info = new JSONObject(new JSONTokener(body));
        assertTrue(info.has("name"), "Response should have a name field");
        assertEquals("mds1491.mbag0_2-0.zip", info.getString("name"));
        assertTrue(info.has("contentLength"), "Response should have contentLength");
        assertTrue(info.getLong("contentLength") > 0, "contentLength should be > 0");
    }

    @Test
    public void testDescribeAIPNotFound() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Get info for non-existent AIP
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/_aip/nonexistent.mbag0_2-0.zip/_info",
            HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    public void testDescribeAIPVersioned() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Get info for versioned AIP
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/_aip/mds1491.1_1_0.mbag0_4-1.zip/_info",
            HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());

        String body = resp.getBody();
        assertNotNull(body);
        logger.info("Versioned AIP info response: " + body);

        JSONObject info = new JSONObject(new JSONTokener(body));
        assertTrue(info.has("name"), "Response should have a name field");
        assertTrue(info.getString("name").contains("mds1491.1_1_0"), "Name should contain version");
    }
}
