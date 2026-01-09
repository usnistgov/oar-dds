package gov.nist.oar.common.datasetaccess.exception;

/**
 * Exception thrown when there's an error communicating with the dataset-access service.
 */
public class DatasetAccessClientException extends RuntimeException {

    private final int statusCode;

    public DatasetAccessClientException(String message) {
        super(message);
        this.statusCode = 500;
    }

    public DatasetAccessClientException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public DatasetAccessClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500;
    }

    public DatasetAccessClientException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Check if this is a "not found" error
     */
    public boolean isNotFound() {
        return statusCode == 404;
    }

    /**
     * Check if this is a service unavailable error
     */
    public boolean isServiceUnavailable() {
        return statusCode == 503 || statusCode == 500;
    }
}
