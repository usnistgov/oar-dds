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
package gov.nist.oar.common.cache.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CacheObjectSummaryTest {

    @Test
    public void testBuilderAndGetters() {
        CacheObjectInfo file1 = CacheObjectInfo.builder()
                .id("mds2-1234/file1.txt")
                .name("file1.txt")
                .cached(true)
                .size(1024L)
                .build();

        CacheObjectInfo file2 = CacheObjectInfo.builder()
                .id("mds2-1234/file2.txt")
                .name("file2.txt")
                .cached(false)
                .size(2048L)
                .build();

        List<CacheObjectInfo> files = Arrays.asList(file1, file2);

        CacheObjectSummary summary = CacheObjectSummary.builder()
                .id("mds2-1234")
                .totalObjects(2)
                .cachedObjects(1)
                .totalSize(3072L)
                .files(files)
                .build();

        assertEquals("mds2-1234", summary.getId());
        assertEquals(2, summary.getTotalObjects());
        assertEquals(1, summary.getCachedObjects());
        assertEquals(3072L, summary.getTotalSize());
        assertEquals(2, summary.getFiles().size());
        assertEquals("file1.txt", summary.getFiles().get(0).getName());
        assertEquals("file2.txt", summary.getFiles().get(1).getName());
    }

    @Test
    public void testNoArgsConstructor() {
        CacheObjectSummary summary = new CacheObjectSummary();
        assertNull(summary.getId());
        assertNull(summary.getTotalObjects());
        assertNull(summary.getCachedObjects());
        assertNull(summary.getTotalSize());
        assertNull(summary.getFiles());
    }

    @Test
    public void testWithMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("version", "1.0");
        metadata.put("lastUpdated", 1609459200000L);

        CacheObjectSummary summary = CacheObjectSummary.builder()
                .id("mds2-5678")
                .metadata(metadata)
                .build();

        assertNotNull(summary.getMetadata());
        assertEquals("1.0", summary.getMetadata().get("version"));
        assertEquals(1609459200000L, summary.getMetadata().get("lastUpdated"));
    }

    @Test
    public void testJsonSerialization() throws Exception {
        CacheObjectSummary summary = CacheObjectSummary.builder()
                .id("test-dataset")
                .totalObjects(10)
                .cachedObjects(5)
                .totalSize(10240L)
                .build();

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(summary);

        assertTrue(json.contains("\"id\":\"test-dataset\""));
        assertTrue(json.contains("\"totalObjects\":10"));
        assertTrue(json.contains("\"cachedObjects\":5"));
        assertTrue(json.contains("\"totalSize\":10240"));
    }

    @Test
    public void testJsonDeserialization() throws Exception {
        String json = "{\"id\":\"mds2-test\",\"totalObjects\":3,\"cachedObjects\":2,\"totalSize\":5120,\"unknownField\":\"ignored\"}";

        ObjectMapper mapper = new ObjectMapper();
        CacheObjectSummary summary = mapper.readValue(json, CacheObjectSummary.class);

        assertEquals("mds2-test", summary.getId());
        assertEquals(3, summary.getTotalObjects());
        assertEquals(2, summary.getCachedObjects());
        assertEquals(5120L, summary.getTotalSize());
    }

    @Test
    public void testJsonWithNestedFiles() throws Exception {
        String json = "{\"id\":\"mds2-test\",\"totalObjects\":1,\"cachedObjects\":1,\"files\":[{\"id\":\"mds2-test/file.txt\",\"name\":\"file.txt\",\"cached\":true}]}";

        ObjectMapper mapper = new ObjectMapper();
        CacheObjectSummary summary = mapper.readValue(json, CacheObjectSummary.class);

        assertEquals("mds2-test", summary.getId());
        assertNotNull(summary.getFiles());
        assertEquals(1, summary.getFiles().size());
        assertEquals("file.txt", summary.getFiles().get(0).getName());
        assertTrue(summary.getFiles().get(0).getCached());
    }

    @Test
    public void testEqualsAndHashCode() {
        CacheObjectSummary summary1 = CacheObjectSummary.builder()
                .id("test-id")
                .totalObjects(5)
                .build();

        CacheObjectSummary summary2 = CacheObjectSummary.builder()
                .id("test-id")
                .totalObjects(5)
                .build();

        assertEquals(summary1, summary2);
        assertEquals(summary1.hashCode(), summary2.hashCode());
    }
}
