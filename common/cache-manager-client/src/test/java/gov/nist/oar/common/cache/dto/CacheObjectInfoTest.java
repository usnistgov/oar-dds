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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CacheObjectInfoTest {

    @Test
    public void testBuilderAndGetters() {
        CacheObjectInfo info = CacheObjectInfo.builder()
                .id("mds2-1234/file.txt")
                .name("file.txt")
                .cached(true)
                .volume("fst0")
                .size(1024L)
                .score(100)
                .checksum("abc123def456")
                .mimetype("text/plain")
                .since(1609459200000L)
                .ngets(5)
                .priority(10)
                .build();

        assertEquals("mds2-1234/file.txt", info.getId());
        assertEquals("file.txt", info.getName());
        assertTrue(info.getCached());
        assertEquals("fst0", info.getVolume());
        assertEquals(1024L, info.getSize());
        assertEquals(100, info.getScore());
        assertEquals("abc123def456", info.getChecksum());
        assertEquals("text/plain", info.getMimetype());
        assertEquals(1609459200000L, info.getSince());
        assertEquals(5, info.getNgets());
        assertEquals(10, info.getPriority());
    }

    @Test
    public void testNoArgsConstructor() {
        CacheObjectInfo info = new CacheObjectInfo();
        assertNull(info.getId());
        assertNull(info.getName());
        assertNull(info.getCached());
        assertNull(info.getVolume());
    }

    @Test
    public void testIsAvailableWhenCached() {
        CacheObjectInfo info = CacheObjectInfo.builder()
                .cached(true)
                .build();

        assertTrue(info.isAvailable());
        assertTrue(info.isCached());
    }

    @Test
    public void testIsAvailableWhenNotCached() {
        CacheObjectInfo info = CacheObjectInfo.builder()
                .cached(false)
                .build();

        assertFalse(info.isAvailable());
        assertFalse(info.isCached());
    }

    @Test
    public void testIsAvailableWhenNull() {
        CacheObjectInfo info = new CacheObjectInfo();
        assertFalse(info.isAvailable());
        assertFalse(info.isCached());
    }

    @Test
    public void testGetVolumeName() {
        CacheObjectInfo info = CacheObjectInfo.builder()
                .volume("gen0")
                .build();

        assertEquals("gen0", info.getVolumeName());
    }

    @Test
    public void testGetContentType() {
        CacheObjectInfo info = CacheObjectInfo.builder()
                .mimetype("application/json")
                .build();

        assertEquals("application/json", info.getContentType());
    }

    @Test
    public void testGetChecksumAlgorithmDefault() {
        CacheObjectInfo info = new CacheObjectInfo();
        assertEquals("sha256", info.getChecksumAlgorithm());
    }

    @Test
    public void testGetChecksumAlgorithmFromMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("checksumAlgorithm", "md5");

        CacheObjectInfo info = CacheObjectInfo.builder()
                .additionalMetadata(metadata)
                .build();

        assertEquals("md5", info.getChecksumAlgorithm());
    }

    @Test
    public void testGetRedirectUrlNull() {
        CacheObjectInfo info = new CacheObjectInfo();
        assertNull(info.getRedirectUrl());
    }

    @Test
    public void testGetRedirectUrlFromMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("redirectUrl", "https://example.com/file.txt");

        CacheObjectInfo info = CacheObjectInfo.builder()
                .additionalMetadata(metadata)
                .build();

        assertEquals("https://example.com/file.txt", info.getRedirectUrl());
    }

    @Test
    public void testJsonSerialization() throws Exception {
        CacheObjectInfo info = CacheObjectInfo.builder()
                .id("mds2-1234/data.csv")
                .name("data.csv")
                .cached(true)
                .size(2048L)
                .mimetype("text/csv")
                .build();

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(info);

        assertTrue(json.contains("\"id\":\"mds2-1234/data.csv\""));
        assertTrue(json.contains("\"name\":\"data.csv\""));
        assertTrue(json.contains("\"cached\":true"));
        assertTrue(json.contains("\"size\":2048"));
        assertTrue(json.contains("\"mimetype\":\"text/csv\""));
    }

    @Test
    public void testJsonDeserialization() throws Exception {
        String json = "{\"id\":\"test-id\",\"name\":\"test.txt\",\"cached\":true,\"size\":512,\"unknownField\":\"ignored\"}";

        ObjectMapper mapper = new ObjectMapper();
        CacheObjectInfo info = mapper.readValue(json, CacheObjectInfo.class);

        assertEquals("test-id", info.getId());
        assertEquals("test.txt", info.getName());
        assertTrue(info.getCached());
        assertEquals(512L, info.getSize());
    }

    @Test
    public void testEqualsAndHashCode() {
        CacheObjectInfo info1 = CacheObjectInfo.builder()
                .id("test-id")
                .name("test.txt")
                .build();

        CacheObjectInfo info2 = CacheObjectInfo.builder()
                .id("test-id")
                .name("test.txt")
                .build();

        assertEquals(info1, info2);
        assertEquals(info1.hashCode(), info2.hashCode());
    }
}
