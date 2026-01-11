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
package gov.nist.oar.common.datasetaccess.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class DatasetAccessClientExceptionTest {

    @Test
    public void testMessageOnlyConstructor() {
        DatasetAccessClientException ex = new DatasetAccessClientException("Dataset access error");

        assertEquals("Dataset access error", ex.getMessage());
        assertEquals(500, ex.getStatusCode());
        assertNull(ex.getCause());
    }

    @Test
    public void testMessageAndStatusCodeConstructor() {
        DatasetAccessClientException ex = new DatasetAccessClientException("Not found", 404);

        assertEquals("Not found", ex.getMessage());
        assertEquals(404, ex.getStatusCode());
        assertNull(ex.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        RuntimeException cause = new RuntimeException("Underlying error");
        DatasetAccessClientException ex = new DatasetAccessClientException("Service error", cause);

        assertEquals("Service error", ex.getMessage());
        assertEquals(500, ex.getStatusCode());
        assertEquals(cause, ex.getCause());
    }

    @Test
    public void testFullConstructor() {
        RuntimeException cause = new RuntimeException("Network timeout");
        DatasetAccessClientException ex = new DatasetAccessClientException("Service unavailable", 503, cause);

        assertEquals("Service unavailable", ex.getMessage());
        assertEquals(503, ex.getStatusCode());
        assertEquals(cause, ex.getCause());
    }

    @Test
    public void testIsNotFoundTrue() {
        DatasetAccessClientException ex = new DatasetAccessClientException("File not found", 404);
        assertTrue(ex.isNotFound());
    }

    @Test
    public void testIsNotFoundFalse() {
        DatasetAccessClientException ex = new DatasetAccessClientException("Server error", 500);
        assertFalse(ex.isNotFound());
    }

    @Test
    public void testIsServiceUnavailable500() {
        DatasetAccessClientException ex = new DatasetAccessClientException("Server error", 500);
        assertTrue(ex.isServiceUnavailable());
    }

    @Test
    public void testIsServiceUnavailable503() {
        DatasetAccessClientException ex = new DatasetAccessClientException("Service unavailable", 503);
        assertTrue(ex.isServiceUnavailable());
    }

    @Test
    public void testIsServiceUnavailableFalse() {
        DatasetAccessClientException ex = new DatasetAccessClientException("Not found", 404);
        assertFalse(ex.isServiceUnavailable());

        DatasetAccessClientException ex2 = new DatasetAccessClientException("Bad request", 400);
        assertFalse(ex2.isServiceUnavailable());
    }

    @Test
    public void testIsRuntimeException() {
        DatasetAccessClientException ex = new DatasetAccessClientException("Test");
        assertTrue(ex instanceof RuntimeException);
    }

    @Test
    public void testCanBeThrown() {
        assertThrows(DatasetAccessClientException.class, () -> {
            throw new DatasetAccessClientException("Thrown exception");
        });
    }

    @Test
    public void testDefaultStatusCodeIs500() {
        DatasetAccessClientException ex1 = new DatasetAccessClientException("Error");
        assertEquals(500, ex1.getStatusCode());

        DatasetAccessClientException ex2 = new DatasetAccessClientException("Error", new RuntimeException());
        assertEquals(500, ex2.getStatusCode());
    }
}
