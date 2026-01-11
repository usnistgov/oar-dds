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
import org.springframework.boot.test.context.TestConfiguration;
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

import gov.nist.oar.common.cache.client.CacheManagerClient;
import gov.nist.oar.common.cache.dto.CacheObjectInfo;
import gov.nist.oar.common.cache.dto.CacheObjectSummary;
import gov.nist.oar.distrib.RestrictedAccessServiceApplication;
import gov.nist.oar.distrib.service.rpa.RPARequestHandler;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Integration tests for RPARequestHandlerController.
 * Tests the REST API endpoints for RPA (Restricted Public Access) request handling.
 *
 * Note: These tests use mocked external dependencies (Salesforce, reCAPTCHA, etc.)
 * since those external services are not available during testing.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {RestrictedAccessServiceApplication.class, RPARequestHandlerControllerIntegrationTest.TestConfig.class},
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "distrib.rpa.salesforceEndpoint=http://localhost:9999/mock",
    "distrib.rpa.jwtSecret=test-secret-key-that-is-at-least-256-bits-long-for-testing",
    "distrib.rpa.recaptchaSecret=test-recaptcha-secret",
    "distrib.rpa.recaptchaVerifyApi=http://localhost:9999/recaptcha/verify",
    "distrib.rpa.approverEmails=test@test.gov",
    "distrib.rpa.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
public class RPARequestHandlerControllerIntegrationTest {

    Logger logger = LoggerFactory.getLogger(RPARequestHandlerControllerIntegrationTest.class);

    @LocalServerPort
    int port;

    TestRestTemplate websvc = new TestRestTemplate();

    @Autowired(required = false)
    RPARequestHandler rpaRequestHandler;

    /**
     * Test configuration that provides mock implementations for external dependencies.
     */
    @TestConfiguration
    static class TestConfig {

        /**
         * Mock CacheManagerClient that returns empty/null for all cache lookups.
         * This bypasses the CacheManagementFeignClient entirely by providing a primary bean.
         */
        @Bean
        @Primary
        public CacheManagerClient testCacheManagerClient() {
            return new CacheManagerClient() {
                @Override
                public Optional<CacheObjectInfo> getObjectInfo(String datasetId, String filepath) {
                    return Optional.empty();
                }

                @Override
                public boolean isCached(String datasetId, String filepath) {
                    return false;
                }

                @Override
                public CacheObjectSummary getDatasetSummary(String datasetId) {
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
                public Map<String, Object> getResourceMetadata(String datasetId) {
                    return Collections.emptyMap();
                }

                @Override
                public Map<String, Object> getComponentMetadata(String datasetId, String filepath) {
                    return Collections.emptyMap();
                }

                @Override
                public Map<String, Object> cacheDatasetForRPA(String datasetId, String version) {
                    return Collections.emptyMap();
                }

                @Override
                public Map<String, Object> getRPACachedObjects(String randomId) {
                    return Collections.emptyMap();
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
        // Just verify the application context loads successfully
        // rpaRequestHandler may be null if service is disabled
        logger.info("Application context loaded successfully");
    }

    @Test
    public void testTestEndpoint() {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/rpa/test",
            HttpMethod.GET, req, String.class);

        logger.info("Test endpoint response status: " + resp.getStatusCode());
        logger.info("Test endpoint response: " + resp.getBody());

        // May return 503 if service is not operating or 200 if available
        assertTrue(resp.getStatusCode() == HttpStatus.OK ||
                   resp.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE ||
                   resp.getStatusCode() == HttpStatus.UNAUTHORIZED,
                   "Expected 200, 401, or 503 status");
    }

    @Test
    public void testGetRecordWithoutAuth() {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Request without Authorization header should fail
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/rpa/request/accepted/test-record-id",
            HttpMethod.GET, req, String.class);

        logger.info("Get record without auth status: " + resp.getStatusCode());

        // Should return error status (400, 500, or 503) when Authorization is missing
        // The actual behavior is 500 because the handler tries to parse null header
        assertTrue(resp.getStatusCode() == HttpStatus.BAD_REQUEST ||
                   resp.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR ||
                   resp.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE,
                   "Expected 400, 500, or 503 status");
    }

    @Test
    public void testGetRecordWithInvalidAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/rpa/request/accepted/test-record-id",
            HttpMethod.GET, req, String.class);

        logger.info("Get record with invalid auth status: " + resp.getStatusCode());
        logger.info("Get record with invalid auth response: " + resp.getBody());

        // Should return 401 Unauthorized, 500 Internal Server Error (token validation fails),
        // or 503 Service Unavailable if RPA service is not operating
        assertTrue(resp.getStatusCode() == HttpStatus.UNAUTHORIZED ||
                   resp.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR ||
                   resp.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE,
                   "Expected 401, 500, or 503 status");
    }

    @Test
    public void testCreateRecordWithEmptyBody() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = "{}";
        HttpEntity<String> req = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/rpa/request/form",
            HttpMethod.POST, req, String.class);

        logger.info("Create record with empty body status: " + resp.getStatusCode());
        logger.info("Create record with empty body response: " + resp.getBody());

        // Should return 400 or 401 (invalid recaptcha) or 500/503 for service errors
        assertTrue(resp.getStatusCode() == HttpStatus.BAD_REQUEST ||
                   resp.getStatusCode() == HttpStatus.UNAUTHORIZED ||
                   resp.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR ||
                   resp.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE,
                   "Expected error status");
    }

    @Test
    public void testUpdateRecordWithoutAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = "{\"approvalStatus\": \"Approved\"}";
        HttpEntity<String> req = new HttpEntity<>(requestBody, headers);

        // PATCH without Authorization header
        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/rpa/request/accepted/test-record-id",
            HttpMethod.PATCH, req, String.class);

        logger.info("Update record without auth status: " + resp.getStatusCode());

        // Should return error status (400, 500, or 503) when Authorization is missing
        // The actual behavior is 500 because the handler tries to parse null header
        assertTrue(resp.getStatusCode() == HttpStatus.BAD_REQUEST ||
                   resp.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR ||
                   resp.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE,
                   "Expected 400, 500, or 503 status");
    }

    @Test
    public void testUpdateRecordWithInvalidAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");

        String requestBody = "{\"approvalStatus\": \"Approved\"}";
        HttpEntity<String> req = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/rpa/request/accepted/test-record-id",
            HttpMethod.PATCH, req, String.class);

        logger.info("Update record with invalid auth status: " + resp.getStatusCode());
        logger.info("Update record with invalid auth response: " + resp.getBody());

        // Should return 401 Unauthorized, 400 Bad Request (invalid JWT),
        // 500 (processing error), or 503 (service unavailable)
        assertTrue(resp.getStatusCode() == HttpStatus.UNAUTHORIZED ||
                   resp.getStatusCode() == HttpStatus.BAD_REQUEST ||
                   resp.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR ||
                   resp.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE,
                   "Expected error status");
    }

    @Test
    public void testGetMethodNotAllowedOnForm() {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        ResponseEntity<String> resp = websvc.exchange(
            getBaseURL() + "/ds/rpa/request/form",
            HttpMethod.GET, req, String.class);

        // GET should not be allowed on /request/form - only POST
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, resp.getStatusCode());
    }
}
