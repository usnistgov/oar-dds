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

import gov.nist.oar.distrib.VersionServiceApplication;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = VersionServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
public class VersionControllerTest {

    Logger logger = LoggerFactory.getLogger(VersionControllerTest.class);

    @LocalServerPort
    int port;

    TestRestTemplate websvc = new TestRestTemplate();
    HttpHeaders headers = new HttpHeaders();

    private String getBaseURL() {
        return "http://localhost:" + port;
    }

    @Test
    public void testGetServiceVersion() throws JSONException {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/",
                                                      HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        String got = resp.getBody();
        logger.info("### version response: " + got);

        JSONObject json = new JSONObject(got);
        assertNotNull(json.getString("serviceName"));
        assertTrue(json.getString("serviceName").length() > 0);
        assertNotNull(json.getString("version"));
        assertTrue(!json.getString("version").equals("unknown"));
        assertTrue(resp.getHeaders().getFirst("Content-Type").startsWith("application/json"));
    }

    @Test
    public void testGetAllVersions() throws JSONException {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/all",
                                                      HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        String got = resp.getBody();
        logger.info("### all versions response: " + got);

        JSONObject json = new JSONObject(got);
        assertEquals("oar-data-distribution-system", json.getString("system"));
        assertNotNull(json.getString("systemVersion"));

        // Services map should exist (services may not be registered in test)
        JSONObject services = json.getJSONObject("services");
        assertNotNull(services);

        // Should have entries for all expected services
        assertTrue(services.has("dataset-access-service"));
        assertTrue(services.has("cache-mgmt-service"));
        assertTrue(services.has("bundle-plan-service"));
        assertTrue(services.has("data-bundle-service"));
        assertTrue(services.has("aip-access-service"));
        assertTrue(services.has("restricted-access-service"));

        assertTrue(resp.getHeaders().getFirst("Content-Type").startsWith("application/json"));
    }

    @Test
    public void testDeleteNotAllowed() throws JSONException {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/",
                                                      HttpMethod.DELETE, req, String.class);
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, resp.getStatusCode());

        req = new HttpEntity<>(null, headers);
        resp = websvc.exchange(getBaseURL() + "/", HttpMethod.POST, req, String.class);
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, resp.getStatusCode());

        req = new HttpEntity<>(null, headers);
        resp = websvc.exchange(getBaseURL() + "/", HttpMethod.PATCH, req, String.class);
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, resp.getStatusCode());
    }

    @Test
    public void testHEAD() throws JSONException {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/",
                                                      HttpMethod.HEAD, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }
}
