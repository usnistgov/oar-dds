package gov.nist.oar.common.cache.exception;

/**
 * Exception thrown when the cache management service is unavailable.
 * <p>
 * This typically indicates network issues, service downtime, or circuit breaker activation.
 * Callers should implement fallback behavior when this exception is thrown.
 */
public class CacheServiceUnavailableException extends CacheClientException {

    /**
     * Create a service unavailable exception with a message.
     *
     * @param message the error message
     */
    public CacheServiceUnavailableException(String message) {
        super(message);
    }

    /**
     * Create a service unavailable exception with a message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public CacheServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
