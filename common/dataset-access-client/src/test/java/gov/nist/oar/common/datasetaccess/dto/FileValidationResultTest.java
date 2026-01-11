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

public class FileValidationResultTest {

    @Test
    public void testBuilderAndGetters() {
        FileValidationResult result = FileValidationResult.builder()
                .datasetId("mds2-1234")
                .filepath("data/file.csv")
                .accessible(true)
                .size(4096L)
                .contentType("text/csv")
                .statusCode(200)
                .build();

        assertEquals("mds2-1234", result.getDatasetId());
        assertEquals("data/file.csv", result.getFilepath());
        assertTrue(result.isAccessible());
        assertEquals(4096L, result.getSize());
        assertEquals("text/csv", result.getContentType());
        assertEquals(200, result.getStatusCode());
        assertNull(result.getErrorMessage());
    }

    @Test
    public void testNoArgsConstructor() {
        FileValidationResult result = new FileValidationResult();
        assertNull(result.getDatasetId());
        assertNull(result.getFilepath());
        assertFalse(result.isAccessible());
        assertEquals(0L, result.getSize());
        assertEquals(0, result.getStatusCode());
    }

    @Test
    public void testSuccessFactory() {
        FileValidationResult result = FileValidationResult.success(
                "mds2-5678",
                "images/photo.jpg",
                1048576L,
                "image/jpeg"
        );

        assertEquals("mds2-5678", result.getDatasetId());
        assertEquals("images/photo.jpg", result.getFilepath());
        assertTrue(result.isAccessible());
        assertEquals(1048576L, result.getSize());
        assertEquals("image/jpeg", result.getContentType());
        assertEquals(200, result.getStatusCode());
        assertNull(result.getErrorMessage());
    }

    @Test
    public void testFailureFactory() {
        FileValidationResult result = FileValidationResult.failure(
                "mds2-9999",
                "missing/file.txt",
                404,
                "File not found"
        );

        assertEquals("mds2-9999", result.getDatasetId());
        assertEquals("missing/file.txt", result.getFilepath());
        assertFalse(result.isAccessible());
        assertEquals(0L, result.getSize());
        assertEquals(404, result.getStatusCode());
        assertEquals("File not found", result.getErrorMessage());
    }

    @Test
    public void testFailureWith500Error() {
        FileValidationResult result = FileValidationResult.failure(
                "mds2-error",
                "broken/file.bin",
                500,
                "Internal server error"
        );

        assertFalse(result.isAccessible());
        assertEquals(500, result.getStatusCode());
        assertEquals("Internal server error", result.getErrorMessage());
    }

    @Test
    public void testJsonSerialization() throws Exception {
        FileValidationResult result = FileValidationResult.success(
                "test-dataset",
                "path/to/file.txt",
                2048L,
                "text/plain"
        );

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(result);

        assertTrue(json.contains("\"datasetId\":\"test-dataset\""));
        assertTrue(json.contains("\"filepath\":\"path/to/file.txt\""));
        assertTrue(json.contains("\"accessible\":true"));
        assertTrue(json.contains("\"size\":2048"));
        assertTrue(json.contains("\"contentType\":\"text/plain\""));
        assertTrue(json.contains("\"statusCode\":200"));
    }

    @Test
    public void testJsonSerializationOfFailure() throws Exception {
        FileValidationResult result = FileValidationResult.failure(
                "test-dataset",
                "bad/file.txt",
                403,
                "Access denied"
        );

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(result);

        assertTrue(json.contains("\"accessible\":false"));
        assertTrue(json.contains("\"statusCode\":403"));
        assertTrue(json.contains("\"errorMessage\":\"Access denied\""));
    }

    @Test
    public void testJsonDeserialization() throws Exception {
        String json = "{\"datasetId\":\"ds-123\",\"filepath\":\"doc.pdf\",\"accessible\":true,\"size\":8192,\"statusCode\":200}";

        ObjectMapper mapper = new ObjectMapper();
        FileValidationResult result = mapper.readValue(json, FileValidationResult.class);

        assertEquals("ds-123", result.getDatasetId());
        assertEquals("doc.pdf", result.getFilepath());
        assertTrue(result.isAccessible());
        assertEquals(8192L, result.getSize());
        assertEquals(200, result.getStatusCode());
    }

    @Test
    public void testJsonDeserializationIgnoresUnknownFields() throws Exception {
        String json = "{\"datasetId\":\"ds-123\",\"filepath\":\"file.txt\",\"accessible\":true,\"size\":100,\"unknownField\":\"ignored\"}";

        ObjectMapper mapper = new ObjectMapper();
        FileValidationResult result = mapper.readValue(json, FileValidationResult.class);

        assertEquals("ds-123", result.getDatasetId());
        assertEquals("file.txt", result.getFilepath());
    }

    @Test
    public void testEqualsAndHashCode() {
        FileValidationResult result1 = FileValidationResult.success(
                "test-ds", "file.txt", 1024L, "text/plain"
        );

        FileValidationResult result2 = FileValidationResult.success(
                "test-ds", "file.txt", 1024L, "text/plain"
        );

        assertEquals(result1, result2);
        assertEquals(result1.hashCode(), result2.hashCode());
    }
}
