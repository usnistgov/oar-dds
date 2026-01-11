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
package gov.nist.oar.common.datasetaccess.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class FileInfoTest {

    @Test
    public void testBuilderAndGetters() {
        FileInfo.ChecksumInfo checksum = FileInfo.ChecksumInfo.builder()
                .hash("abc123def456")
                .algorithm("sha256")
                .build();

        FileInfo info = FileInfo.builder()
                .name("data.csv")
                .contentLength(4096L)
                .contentType("text/csv")
                .checksum(checksum)
                .aipId("mds2-1234")
                .downloadUrl("https://example.com/data.csv")
                .sinceVersion("1.0.0")
                .build();

        assertEquals("data.csv", info.getName());
        assertEquals(4096L, info.getContentLength());
        assertEquals("text/csv", info.getContentType());
        assertNotNull(info.getChecksum());
        assertEquals("abc123def456", info.getChecksum().getHash());
        assertEquals("sha256", info.getChecksum().getAlgorithm());
        assertEquals("mds2-1234", info.getAipId());
        assertEquals("https://example.com/data.csv", info.getDownloadUrl());
        assertEquals("1.0.0", info.getSinceVersion());
    }

    @Test
    public void testNoArgsConstructor() {
        FileInfo info = new FileInfo();
        assertNull(info.getName());
        assertEquals(0L, info.getContentLength());
        assertNull(info.getContentType());
        assertNull(info.getChecksum());
    }

    @Test
    public void testChecksumInfoBuilder() {
        FileInfo.ChecksumInfo checksum = FileInfo.ChecksumInfo.builder()
                .hash("xyz789")
                .algorithm("md5")
                .build();

        assertEquals("xyz789", checksum.getHash());
        assertEquals("md5", checksum.getAlgorithm());
    }

    @Test
    public void testChecksumInfoNoArgsConstructor() {
        FileInfo.ChecksumInfo checksum = new FileInfo.ChecksumInfo();
        assertNull(checksum.getHash());
        assertNull(checksum.getAlgorithm());
    }

    @Test
    public void testJsonSerialization() throws Exception {
        FileInfo.ChecksumInfo checksum = FileInfo.ChecksumInfo.builder()
                .hash("checksum123")
                .algorithm("sha256")
                .build();

        FileInfo info = FileInfo.builder()
                .name("test.txt")
                .contentLength(1024L)
                .contentType("text/plain")
                .checksum(checksum)
                .aipId("test-aip")
                .build();

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(info);

        assertTrue(json.contains("\"name\":\"test.txt\""));
        assertTrue(json.contains("\"contentLength\":1024"));
        assertTrue(json.contains("\"contentType\":\"text/plain\""));
        assertTrue(json.contains("\"checksum\":{"));
        assertTrue(json.contains("\"hash\":\"checksum123\""));
        assertTrue(json.contains("\"algorithm\":\"sha256\""));
        assertTrue(json.contains("\"aipid\":\"test-aip\""));
    }

    @Test
    public void testJsonDeserialization() throws Exception {
        String json = "{\"name\":\"file.pdf\",\"contentLength\":2048,\"contentType\":\"application/pdf\",\"aipid\":\"aip-123\",\"downloadURL\":\"http://download.example.com/file.pdf\"}";

        ObjectMapper mapper = new ObjectMapper();
        FileInfo info = mapper.readValue(json, FileInfo.class);

        assertEquals("file.pdf", info.getName());
        assertEquals(2048L, info.getContentLength());
        assertEquals("application/pdf", info.getContentType());
        assertEquals("aip-123", info.getAipId());
        assertEquals("http://download.example.com/file.pdf", info.getDownloadUrl());
    }

    @Test
    public void testJsonDeserializationWithChecksum() throws Exception {
        String json = "{\"name\":\"file.bin\",\"contentLength\":512,\"checksum\":{\"hash\":\"def456\",\"algorithm\":\"sha256\"}}";

        ObjectMapper mapper = new ObjectMapper();
        FileInfo info = mapper.readValue(json, FileInfo.class);

        assertEquals("file.bin", info.getName());
        assertEquals(512L, info.getContentLength());
        assertNotNull(info.getChecksum());
        assertEquals("def456", info.getChecksum().getHash());
        assertEquals("sha256", info.getChecksum().getAlgorithm());
    }

    @Test
    public void testJsonDeserializationIgnoresUnknownFields() throws Exception {
        String json = "{\"name\":\"test.txt\",\"contentLength\":100,\"unknownField\":\"shouldBeIgnored\",\"anotherUnknown\":123}";

        ObjectMapper mapper = new ObjectMapper();
        FileInfo info = mapper.readValue(json, FileInfo.class);

        assertEquals("test.txt", info.getName());
        assertEquals(100L, info.getContentLength());
    }

    @Test
    public void testEqualsAndHashCode() {
        FileInfo info1 = FileInfo.builder()
                .name("test.txt")
                .contentLength(1024L)
                .build();

        FileInfo info2 = FileInfo.builder()
                .name("test.txt")
                .contentLength(1024L)
                .build();

        assertEquals(info1, info2);
        assertEquals(info1.hashCode(), info2.hashCode());
    }

    @Test
    public void testChecksumEqualsAndHashCode() {
        FileInfo.ChecksumInfo cs1 = FileInfo.ChecksumInfo.builder()
                .hash("abc123")
                .algorithm("sha256")
                .build();

        FileInfo.ChecksumInfo cs2 = FileInfo.ChecksumInfo.builder()
                .hash("abc123")
                .algorithm("sha256")
                .build();

        assertEquals(cs1, cs2);
        assertEquals(cs1.hashCode(), cs2.hashCode());
    }
}
