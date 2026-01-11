package gov.nist.oar.common.datasetaccess.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the result of validating a file for bundling.
 * <p>
 * This is used by bundle services to check if a file is accessible
 * and get its size before including it in a bundle plan.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileValidationResult {

    /**
     * The dataset ID
     */
    private String datasetId;

    /**
     * The file path within the dataset
     */
    private String filepath;

    /**
     * Whether the file exists and is accessible
     */
    private boolean accessible;

    /**
     * The file size in bytes (0 if not accessible)
     */
    private long size;

    /**
     * The content type of the file
     */
    private String contentType;

    /**
     * HTTP status code from the validation check
     */
    private int statusCode;

    /**
     * Error message if the file is not accessible
     */
    private String errorMessage;

    /**
     * Create a successful validation result
     */
    public static FileValidationResult success(String datasetId, String filepath, long size, String contentType) {
        return FileValidationResult.builder()
                .datasetId(datasetId)
                .filepath(filepath)
                .accessible(true)
                .size(size)
                .contentType(contentType)
                .statusCode(200)
                .build();
    }

    /**
     * Create a failed validation result
     */
    public static FileValidationResult failure(String datasetId, String filepath, int statusCode, String errorMessage) {
        return FileValidationResult.builder()
                .datasetId(datasetId)
                .filepath(filepath)
                .accessible(false)
                .size(0)
                .statusCode(statusCode)
                .errorMessage(errorMessage)
                .build();
    }
}
