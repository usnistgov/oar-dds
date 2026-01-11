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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;
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
import org.springframework.util.FileSystemUtils;

import gov.nist.oar.distrib.CacheManagementServiceApplication;
import gov.nist.oar.distrib.ResourceNotFoundException;
import gov.nist.oar.distrib.StorageVolumeException;
import gov.nist.oar.distrib.cachemgr.CacheManagementException;
import gov.nist.oar.distrib.cachemgr.pdr.PDRCacheManager;
import gov.nist.oar.distrib.web.CacheManagerProvider;
import gov.nist.oar.distrib.web.ConfigurationException;

/**
 * Integration tests for CacheManagementController.
 * Uses @SpringBootTest to load full application context with cache manager configured.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = CacheManagementServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "distrib.bagstore.mode=local",
    "distrib.bagstore.location=${basedir}/src/test/resources",
    "distrib.baseurl=http://localhost/cache",
    "distrib.cachemgr.admindir=${java.io.tmpdir}/testcmgr",
    "distrib.cachemgr.headbagCacheSize=40000000",
    "distrib.cachemgr.checkDutyCycle=3",
    "distrib.cachemgr.dbrootdir=${java.io.tmpdir}/testcmgr/db",
    "distrib.cachemgr.volumes[0].location=file://vols/king",
    "distrib.cachemgr.volumes[0].name=king",
    "distrib.cachemgr.volumes[0].capacity=30000000",
    "distrib.cachemgr.volumes[1].location=file://vols/pratt",
    "distrib.cachemgr.volumes[1].name=pratt",
    "distrib.cachemgr.volumes[1].capacity=36000000",
    "distrib.cachemgr.volumes[0].roles[0]=small",
    "distrib.cachemgr.volumes[0].roles[1]=fast",
    "distrib.cachemgr.volumes[1].roles[0]=large",
    "distrib.cachemgr.volumes[1].roles[1]=general",
    "distrib.cachemgr.restapi.accesstoken=SECRET",
    "cloud.aws.region=us-east-1",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
public class CacheManagementControllerIntegrationTest {

    Logger logger = LoggerFactory.getLogger(CacheManagementControllerIntegrationTest.class);

    @LocalServerPort
    int port;

    TestRestTemplate websvc = new TestRestTemplate();
    HttpHeaders headers = new HttpHeaders();

    static File testdir = null;

    @Autowired
    CacheManagerProvider provider;

    public CacheManagementControllerIntegrationTest() {
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer SECRET");
    }

    @BeforeAll
    public static void setUpClass() throws IOException {
        String tmpdir = System.getProperty("java.io.tmpdir");
        if (tmpdir == null)
            throw new RuntimeException("java.io.tmpdir property not set");
        File tmp = new File(tmpdir);
        if (! tmp.exists())
            tmp.mkdir();
        testdir = new File(tmp, "testcmgr");
        // Clean up any previous test runs
        if (testdir.exists())
            FileSystemUtils.deleteRecursively(testdir);
        testdir.mkdirs();
        (new File(testdir,"db")).mkdirs();
        (new File(testdir,"headbags")).mkdirs();
    }

    @AfterAll
    public static void tearDownClass() throws IOException {
        if (testdir != null && testdir.exists())
            FileSystemUtils.deleteRecursively(testdir);
    }

    @BeforeEach
    public void setUp() throws CacheManagementException, StorageVolumeException, ResourceNotFoundException, ConfigurationException {
        // Cache the mds1491 dataset for tests if not already cached
        if (provider != null && provider.canProvideManager()) {
            try {
                provider.getPDRCacheManager().cacheDataset("mds1491", null, true, 0 , null);
            } catch (Exception e) {
                // Dataset may already be cached from a previous test
                logger.debug("Dataset may already be cached: " + e.getMessage());
            }
        }
    }

    private String getBaseURL() {
        return "http://localhost:" + port;
    }

    @Test
    public void testConfig() {
        assertTrue(provider.canProvideManager(), "Provider should be able to create manager");
        File db = new File(testdir, "db/data.sqlite");
        assertTrue(db.isFile(), "Database file should exist at: " + db.getAbsolutePath());
    }

    @Test
    public void testGetStatus() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/cache/", HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONAssert.assertEquals("{requestURL:\"/cache/\"," +
                                 "status:200,message:\"Cache Manager in Use\",method:GET}",
                                resp.getBody(), true);
    }

    @Test
    public void testSummarizeVolumes() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/cache/volumes/", HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONArray summary = new JSONArray(new JSONTokener(resp.getBody()));
        assertEquals(2, summary.length());
    }

    @Test
    public void testSummarizeVolume() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Non-existent volume should return 404
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/cache/volumes/goober", HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());

        // King volume should exist with cached files
        resp = websvc.exchange(getBaseURL() + "/cache/volumes/king", HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject summary = new JSONObject(new JSONTokener(resp.getBody()));
        assertTrue(summary.getInt("filecount") >= 3, "King volume should have at least 3 files");
        assertEquals(30000000, summary.getLong("capacity"));

        // Pratt volume should be empty
        resp = websvc.exchange(getBaseURL() + "/cache/volumes/pratt", HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        summary = new JSONObject(new JSONTokener(resp.getBody()));
        assertEquals(0, summary.getInt("filecount"));
        assertEquals(0, summary.getInt("totalsize"));
        assertEquals(36000000, summary.getLong("capacity"));
    }

    @Test
    public void testSummarizeContents() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/cache/objects/", HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONArray summary = new JSONArray(new JSONTokener(resp.getBody()));
        assertEquals(1, summary.length());
        assertEquals("3A1EE2F169DD3B8CE0531A570681DB5D1491", summary.getJSONObject(0).optString("aipid", null));
        assertEquals(3, summary.getJSONObject(0).optInt("filecount", 0));
    }

    @Test
    public void testSummarizeDataset() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Non-existent dataset should return 404
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/cache/objects/goober", HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());

        // mds1491 should exist
        resp = websvc.exchange(getBaseURL() + "/cache/objects/mds1491", HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject summary = new JSONObject(new JSONTokener(resp.getBody()));
        assertEquals("3A1EE2F169DD3B8CE0531A570681DB5D1491", summary.optString("aipid", null));
        assertEquals(3, summary.getJSONArray("files").length());
    }

    @Test
    public void testListObjectsFor() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Non-existent dataset
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/cache/objects/goober/:checked", HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());

        // List files for mds1491
        resp = websvc.exchange(getBaseURL() + "/cache/objects/mds1491/:files", HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONArray summary = new JSONArray(new JSONTokener(resp.getBody()));
        assertTrue(summary.length() >= 3, "Should have at least 3 files");
    }

    @Test
    public void testGetSpecificFile() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Non-existent file path
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/cache/objects/goober/gurn/1", HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());

        // Specific file listing - verifies we get a non-empty array and can find our file
        resp = websvc.exchange(getBaseURL() + "/cache/objects/mds1491/trial1.json/:files", HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONArray summary = new JSONArray(new JSONTokener(resp.getBody()));
        assertTrue(summary.length() >= 1, "Should have at least 1 file");
        // Look for our specific file in the results
        boolean foundFile = false;
        for (int i = 0; i < summary.length(); i++) {
            if ("mds1491/trial1.json".equals(summary.getJSONObject(i).optString("name"))) {
                foundFile = true;
                break;
            }
        }
        assertTrue(foundFile, "Should find mds1491/trial1.json in results");

        // Get specific file info
        resp = websvc.exchange(getBaseURL() + "/cache/objects/mds1491/trial1.json", HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject file = new JSONObject(new JSONTokener(resp.getBody()));
        assertEquals("mds1491/trial1.json", file.getString("name"));
    }

    @Test
    public void testRemoveFromCache() {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Verify dataset is cached
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/cache/objects/mds1491/:cached",
                HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        // Remove a single file
        ResponseEntity<String> removeResp = websvc.exchange(getBaseURL() +
                        "/cache/objects/mds1491/trial2.json/:cached",
                HttpMethod.DELETE, req, String.class);
        assertEquals(HttpStatus.OK, removeResp.getStatusCode());

        // Verify file is removed
        resp = websvc.exchange(getBaseURL() + "/cache/objects/mds1491/trial2.json/:cached",
                HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());

        // Remove entire dataset
        removeResp = websvc.exchange(getBaseURL() +
                        "/cache/objects/mds1491/:cached",
                HttpMethod.DELETE, req, String.class);
        assertEquals(HttpStatus.OK, removeResp.getStatusCode());

        // Verify dataset is removed
        resp = websvc.exchange(getBaseURL() + "/cache/objects/mds1491/:cached",
                HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    public void testCacheDataset() throws Exception {
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Test caching non-existent dataset
        ResponseEntity<String> resp = websvc.exchange(getBaseURL() + "/cache/objects/goober/gurn/:cached",
                                                      HttpMethod.PUT, req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());

        // Verify another dataset is not cached yet
        resp = websvc.exchange(getBaseURL() + "/cache/objects/67C783D4BA814C8EE05324570681708A1899/:cached",
                               HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());

        // Cache this dataset
        resp = websvc.exchange(getBaseURL() + "/cache/objects/67C783D4BA814C8EE05324570681708A1899/:cached",
                               HttpMethod.PUT, req, String.class);
        assertEquals(HttpStatus.ACCEPTED, resp.getStatusCode());

        // Wait for caching to complete
        for(int i=0; i < 10; i++) {
            try { Thread.sleep(200); } catch (InterruptedException ex) { }
            resp = websvc.exchange(getBaseURL() +
                                "/cache/objects/67C783D4BA814C8EE05324570681708A1899/NMRRVocab20171102.rdf",
                                   HttpMethod.GET, req, String.class);
            if (! HttpStatus.NOT_FOUND.equals(resp.getStatusCode())) break;
        }
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        JSONObject file = new JSONObject(new JSONTokener(resp.getBody()));
        assertEquals("67C783D4BA814C8EE05324570681708A1899/NMRRVocab20171102.rdf", file.getString("name"));

        // Check mds1491 is still cached
        resp = websvc.exchange(getBaseURL() + "/cache/objects/mds1491/trial1.json",
                               HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        file = new JSONObject(new JSONTokener(resp.getBody()));
        assertEquals("mds1491/trial1.json", file.getString("name"));
        long since = file.optLong("since", 0L);
        assertTrue(since > 0L);

        // Test recache=false (should not re-cache)
        resp = websvc.exchange(getBaseURL() + "/cache/objects/mds1491/:cached?recache=false",
                               HttpMethod.PUT, req, String.class);
        assertEquals(HttpStatus.ACCEPTED, resp.getStatusCode());
        try { Thread.sleep(200); } catch (InterruptedException ex) { }

        resp = websvc.exchange(getBaseURL() + "/cache/objects/mds1491/trial1.json",
                               HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        file = new JSONObject(new JSONTokener(resp.getBody()));
        assertEquals("mds1491/trial1.json", file.getString("name"));
        assertEquals(since, file.optLong("since", 0L));

        // Test recache=true (should re-cache with new timestamp)
        resp = websvc.exchange(getBaseURL() + "/cache/objects/mds1491/:cached?recache=true",
                               HttpMethod.PUT, req, String.class);
        assertEquals(HttpStatus.ACCEPTED, resp.getStatusCode());

        for(int i=0; i < 10; i++) {
            try { Thread.sleep(200); } catch (InterruptedException ex) { }
            resp = websvc.exchange(getBaseURL() + "/cache/objects/mds1491/trial1.json",
                                   HttpMethod.GET, req, String.class);
            if (! HttpStatus.OK.equals(resp.getStatusCode())) break;
            file = new JSONObject(new JSONTokener(resp.getBody()));
            if (since < file.optLong("since", 0L)) break;
        }
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        file = new JSONObject(new JSONTokener(resp.getBody()));
        assertEquals("mds1491/trial1.json", file.getString("name"));
        assertTrue( since < file.optLong("since", 0L), "Cache object's since date is too old: " +
                    file.optLong("since", 0L) + " <= " + since);
    }

    @Test
    public void testRunMonitor() throws Exception {
        JSONObject status = null;
        ResponseEntity<String> resp = null;
        HttpEntity<String> req = new HttpEntity<>(null, headers);

        // Check that monitor status endpoint works
        resp = websvc.exchange(getBaseURL() + "/cache/monitor/", HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        status = new JSONObject(new JSONTokener(resp.getBody()));
        // Note: lastRan and lastRanDate may have state from previous tests, so we just verify the response has these fields
        assertTrue(status.has("lastRan"));
        assertTrue(status.has("lastRanDate"));

        // Start a single monitor run
        PDRCacheManager mgr = provider.getPDRCacheManager();
        PDRCacheManager.MonitorThread month = mgr.getMonitorThread();
        resp = websvc.exchange(getBaseURL() + "/cache/monitor/running", HttpMethod.PUT, req, String.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertFalse(month.isContinuous());
        try { month.join(5000); } catch (InterruptedException ex) {  }

        resp = websvc.exchange(getBaseURL() + "/cache/monitor/running", HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());

        // Keep polling /cache/monitor/ until the job completes
        int attempts = 0;
        while (attempts++ < 10) {
            resp = websvc.exchange(getBaseURL() + "/cache/monitor/", HttpMethod.GET, req, String.class);
            assertEquals(HttpStatus.OK, resp.getStatusCode());

            status = new JSONObject(resp.getBody());
            if (!status.getBoolean("running")) {
                assertTrue(status.getLong("lastRan") > 0);
                assertNotEquals("(never)", status.getString("lastRanDate"));
                return;  // success
            }
            Thread.sleep(100);
        }
        fail("Monitor failed to finish?");
    }

    @Test
    public void testStartMonitor() throws Exception {
        JSONObject status = null;
        ResponseEntity<String> resp = null;
        HttpEntity<String> req = new HttpEntity<String>(null, headers);

        // Check that monitor status endpoint works
        resp = websvc.exchange(getBaseURL() + "/cache/monitor/", HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        status = new JSONObject(new JSONTokener(resp.getBody()));
        // Note: lastRan and lastRanDate may have state from previous tests, so we just verify the response has these fields
        assertTrue(status.has("lastRan"));
        assertTrue(status.has("lastRanDate"));

        // Start monitor in continuous mode
        PDRCacheManager mgr = provider.getPDRCacheManager();
        PDRCacheManager.MonitorThread month = mgr.getMonitorThread();
        resp = websvc.exchange(getBaseURL() + "/cache/monitor/running?repeat=1", HttpMethod.PUT, req, String.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertTrue(month.isContinuous());
        assertTrue(month.isAlive());

        // Monitor should now be running
        resp = websvc.exchange(getBaseURL() + "/cache/monitor/running", HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        // Stop the monitor
        month = mgr.getMonitorThread();
        resp = websvc.exchange(getBaseURL() + "/cache/monitor/running?repeat=0", HttpMethod.PUT, req, String.class);
        assertEquals(HttpStatus.ACCEPTED, resp.getStatusCode());
        assertFalse(month.isContinuous());
        try { month.join(5000); } catch (InterruptedException ex) {  }
        assertFalse(month.isAlive());

        resp = websvc.exchange(getBaseURL() + "/cache/monitor/running", HttpMethod.GET, req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());

        // Keep polling /cache/monitor/ until the job completes
        int attempts = 0;
        while (attempts++ < 10) {
            resp = websvc.exchange(getBaseURL() + "/cache/monitor/", HttpMethod.GET, req, String.class);
            assertEquals(HttpStatus.OK, resp.getStatusCode());

            status = new JSONObject(resp.getBody());
            if (!status.getBoolean("running")) {
                assertTrue(status.getLong("lastRan") > 0);
                assertNotEquals("(never)", status.getString("lastRanDate"));
                return;  // success
            }
            Thread.sleep(100);
        }
        fail("Monitor failed to finish?");
    }
}
