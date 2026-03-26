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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import gov.nist.oar.distrib.CacheManagementServiceApplication;
import gov.nist.oar.distrib.web.CacheManagerProvider;

/**
 * Tests for CacheManagementController when cache management is not configured.
 * When no cache volumes are configured, the controller should return 404 for all endpoints.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = CacheManagementServiceApplication.class,
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "distrib.bagstore.mode=local",
    "distrib.bagstore.location=${basedir}/src/test/resources",
    "distrib.baseurl=http://localhost/oar-distrib-service",
    "distrib.cachemgr.restapi.accesstoken=SECRET",
    "cloud.aws.region=us-east-1",
    // Note: No cache volumes configured - this should cause cache management to be disabled
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
public class NoCacheManagementControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate websvc;

    private final HttpHeaders headers = new HttpHeaders();

    @Autowired(required = false)
    private CacheManagerProvider provider;

    public NoCacheManagementControllerTest() {
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer SECRET");
    }

    private String getBaseURL() {
        return "http://localhost:" + port;
    }

    @Test
    public void testProviderCannotProvideManager() {
        // When no cache volumes are configured, provider should not be able to provide manager
        if (provider != null) {
            assertFalse(provider.canProvideManager(),
                "Provider should not be able to provide manager when no volumes configured");
        }
    }

    @Test
    public void testGetStatusReturns404() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/cache/",
                                                      HttpMethod.GET, req, String.class);

        // When cache is not configured, should return 404
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());

        String body = resp.getBody();
        JSONObject error = new JSONObject(new JSONTokener(body));
        assertEquals(404, error.getInt("status"));
        assertTrue(error.getString("message").contains("not in operation") ||
                   error.getString("message").contains("not configured") ||
                   error.getString("message").contains("Not Found"),
                   "Error message should indicate cache not operational");
    }

    @Test
    public void testSummarizeVolumesReturns404() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/cache/volumes/",
                                                      HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());

        resp = websvc.exchange(getBaseURL() + "/cache/volumes",
                               HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    public void testSummarizeVolumeReturns404() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/cache/volumes/goober",
                                                      HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    public void testSummarizeContentsReturns404() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/cache/objects/",
                                                      HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    public void testListObjectsForReturns404() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/cache/objects/goober",
                                                      HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    public void testListObjectsCheckedReturns404() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/cache/objects/goober/:checked",
                                                      HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    public void testListObjectsWithPathReturns404() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/cache/objects/goober/gurn/1",
                                                      HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }
}
