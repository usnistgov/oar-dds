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
package gov.nist.oar.common.cache.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class CacheClientExceptionTest {

    @Test
    public void testMessageOnlyConstructor() {
        CacheClientException ex = new CacheClientException("Cache error occurred");

        assertEquals("Cache error occurred", ex.getMessage());
        assertNull(ex.getCause());
        assertNull(ex.getStatusCode());
        assertNull(ex.getObjectId());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        RuntimeException cause = new RuntimeException("Underlying error");
        CacheClientException ex = new CacheClientException("Cache error", cause);

        assertEquals("Cache error", ex.getMessage());
        assertEquals(cause, ex.getCause());
        assertEquals("Underlying error", ex.getCause().getMessage());
        assertNull(ex.getStatusCode());
        assertNull(ex.getObjectId());
    }

    @Test
    public void testFullConstructor() {
        RuntimeException cause = new RuntimeException("Network error");
        CacheClientException ex = new CacheClientException(
                "Failed to retrieve cache object",
                cause,
                404,
                "mds2-1234/file.txt"
        );

        assertEquals("Failed to retrieve cache object", ex.getMessage());
        assertEquals(cause, ex.getCause());
        assertEquals(404, ex.getStatusCode());
        assertEquals("mds2-1234/file.txt", ex.getObjectId());
    }

    @Test
    public void testFullConstructorWithNullValues() {
        CacheClientException ex = new CacheClientException("Error", null, null, null);

        assertEquals("Error", ex.getMessage());
        assertNull(ex.getCause());
        assertNull(ex.getStatusCode());
        assertNull(ex.getObjectId());
    }

    @Test
    public void testIsRuntimeException() {
        CacheClientException ex = new CacheClientException("Test");
        assertTrue(ex instanceof RuntimeException);
    }

    @Test
    public void testCanBeThrown() {
        assertThrows(CacheClientException.class, () -> {
            throw new CacheClientException("Thrown exception");
        });
    }

    @Test
    public void testWithDifferentStatusCodes() {
        CacheClientException ex500 = new CacheClientException("Server error", null, 500, "obj-1");
        CacheClientException ex503 = new CacheClientException("Service unavailable", null, 503, "obj-2");
        CacheClientException ex400 = new CacheClientException("Bad request", null, 400, "obj-3");

        assertEquals(500, ex500.getStatusCode());
        assertEquals(503, ex503.getStatusCode());
        assertEquals(400, ex400.getStatusCode());
    }
}
