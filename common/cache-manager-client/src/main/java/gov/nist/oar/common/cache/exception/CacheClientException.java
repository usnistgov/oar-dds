package gov.nist.oar.common.cache.exception;

/**
 * Base exception for all cache client errors.
 * <p>
 * This exception is thrown when communication with the cache management
 * service fails or returns an error response.
 */
public class CacheClientException extends RuntimeException {

    private final Integer statusCode;
    private final String objectId;

    /**
     * Create a cache client exception with a message.
     *
     * @param message the error message
     */
    public CacheClientException(String message) {
        super(message);
        this.statusCode = null;
        this.objectId = null;
    }

    /**
     * Create a cache client exception with a message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public CacheClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
        this.objectId = null;
    }

    /**
     * Create a cache client exception with full details.
     *
     * @param message the error message
     * @param cause the underlying cause
     * @param statusCode HTTP status code from the service
     * @param objectId the object ID that was being operated on
     */
    public CacheClientException(String message, Throwable cause, Integer statusCode, String objectId) {
        super(message, cause);
        this.statusCode = statusCode;
        this.objectId = objectId;
    }

    /**
     * Get the HTTP status code from the cache service.
     *
     * @return the status code, or null if not available
     */
    public Integer getStatusCode() {
        return statusCode;
    }

    /**
     * Get the object ID that was being operated on.
     *
     * @return the object ID, or null if not applicable
     */
    public String getObjectId() {
        return objectId;
    }
}
