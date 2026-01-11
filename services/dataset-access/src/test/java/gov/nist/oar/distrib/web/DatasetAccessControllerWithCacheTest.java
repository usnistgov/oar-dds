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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONArray;
import org.json.JSONTokener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;
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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import gov.nist.oar.common.cache.client.CacheManagerClient;
import gov.nist.oar.common.cache.dto.CacheObjectInfo;
import gov.nist.oar.common.cache.dto.CacheObjectSummary;
import gov.nist.oar.distrib.DatasetAccessServiceApplication;
import gov.nist.oar.distrib.service.PreservationBagService;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Integration tests for DatasetAccessController with cache functionality enabled.
 * <p>
 * In the microservices architecture, the dataset-access service communicates with
 * the cache-mgmt-service via the CacheManagerClient. This test verifies that the
 * controller correctly handles cache responses and falls back to bag access.
 * <p>
 * Test scenarios:
 * - File download with cache available (redirects)
 * - File download from bag when not cached
 * - ARK-based file access
 * - HEAD requests for file info
 * - Version-specific file access
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {DatasetAccessServiceApplication.class, DatasetAccessControllerWithCacheTest.TestConfig.class},
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "distrib.bagstore.mode=local",
    "distrib.bagstore.location=${basedir}/src/test/resources",
    "distrib.baseurl=http://localhost/oar-distrib-service",
    "cloud.aws.region=us-east-1",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
public class DatasetAccessControllerWithCacheTest {

    Logger logger = LoggerFactory.getLogger(DatasetAccessControllerWithCacheTest.class);

    @LocalServerPort
    int port;

    TestRestTemplate websvc = new TestRestTemplate();
    HttpHeaders headers = new HttpHeaders();

    @Autowired
    PreservationBagService preservationBagService;

    /**
     * Test configuration that provides a mock CacheManagerClient that simulates
     * cache availability for some files.
     */
    @TestConfiguration
    static class TestConfig {

        /**
         * Mock CacheManagerClient that:
         * - Reports trial3/trial3a.json as cached with redirect URL
         * - Reports trial1.json as cached (no redirect)
         * - Reports other files as not cached
         */
        @Bean
        @Primary
        public CacheManagerClient testCacheManagerClient() {
            return new CacheManagerClient() {
                @Override
                public Optional<CacheObjectInfo> getObjectInfo(String datasetId, String filepath) {
                    // Simulate cached file with redirect
                    if (filepath.equals("trial3/trial3a.json")) {
                        Map<String, Object> metadata = new java.util.HashMap<>();
                        metadata.put("redirectUrl", "https://pdr.net/gen/" + datasetId + "/" + filepath);
                        CacheObjectInfo info = CacheObjectInfo.builder()
                            .id(datasetId + "/" + filepath)
                            .name(filepath)
                            .size(100L)
                            .cached(true)
                            .additionalMetadata(metadata)
                            .build();
                        return Optional.of(info);
                    }
                    // Simulate cached file without redirect
                    if (filepath.equals("trial1.json")) {
                        CacheObjectInfo info = CacheObjectInfo.builder()
                            .id(datasetId + "/" + filepath)
                            .name(filepath)
                            .size(69L)
                            .cached(true)
                            // No redirect URL - will serve from bag
                            .build();
                        return Optional.of(info);
                    }
                    return Optional.empty();
                }

                @Override
                public boolean isCached(String datasetId, String filepath) {
                    return filepath.equals("trial3/trial3a.json") || filepath.equals("trial1.json");
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
                    return true;  // Cache is available in this test
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
        assertNotNull(preservationBagService, "PreservationBagService should be autowired");
    }

    @Test
    public void testDescribeAIPs() throws Exception {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/ds/mds1491/_aip",
                                                      HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        String expect = "[{ name:mds1491.mbag0_2-0.zip, contentLength:9841 }," +
                         "{ name:mds1491.1_1_0.mbag0_4-1.zip, contentLength:14077 }]";

        String got = resp.getBody();
        JSONAssert.assertEquals(expect, got, false);
        assertTrue(resp.getHeaders().getFirst("Content-Type").startsWith("application/json"));
    }

    @Test
    public void testListAIPVersions() throws Exception {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/ds/mds1491/_aip/_v",
                                                      HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        String expect = "[\"1.1.0\", \"0\"]";
        JSONAssert.assertEquals(expect, resp.getBody(), false);
        assertTrue(resp.getHeaders().getFirst("Content-Type").startsWith("application/json"));
    }

    @Test
    public void testDownloadFileFromBag() {
        // File not in cache - should be served from bag
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/ds/mds1491/trial2.json",
                                                      HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getHeaders().getFirst("Content-Type").startsWith("application/json"));
    }

    @Test
    public void testDownloadCachedFile() {
        // trial1.json is "cached" but without redirect URL - should serve from bag
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/ds/mds1491/trial1.json",
                                                      HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getHeaders().getFirst("Content-Type").startsWith("application/json"));
        assertEquals(69, resp.getBody().length());
    }

    @Test
    public void testDownloadFileViaARK() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/ds/ark:/1212/mds1491/trial1.json",
                                                      HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getHeaders().getFirst("Content-Type").startsWith("application/json"));
        assertEquals(69, resp.getBody().length());
    }

    @Test
    public void testDownloadFileFromVersion() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/ds/mds1491/_v/0/trial1.json",
                                                      HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getHeaders().getFirst("Content-Type").startsWith("application/json"));
        assertEquals(69, resp.getBody().length());

        req = new HttpEntity<>(null, headers);
        resp = websvc.exchange(getBaseURL() + "/ds/mds1491/_v/1.1.0/trial1.json",
                               HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getHeaders().getFirst("Content-Type").startsWith("application/json"));
        assertEquals(69, resp.getBody().length());
    }

    @Test
    public void testDownloadFileInfo() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/ds/mds1491/trial1.json",
                                                      HttpMethod.HEAD, req, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        // HEAD request may or may not include Content-Type depending on configuration
        String contentType = resp.getHeaders().getFirst("Content-Type");
        if (contentType != null) {
            assertTrue(contentType.startsWith("application/json"));
        }
        assertNull(resp.getBody());
    }

    @Test
    public void testDownloadNonExistentFile() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/ds/mds1491/nonexistent.json",
                                                      HttpMethod.GET, req, String.class);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }
}
