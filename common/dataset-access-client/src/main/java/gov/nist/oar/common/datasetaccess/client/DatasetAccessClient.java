package gov.nist.oar.common.datasetaccess.client;

import gov.nist.oar.common.datasetaccess.dto.FileInfo;
import gov.nist.oar.common.datasetaccess.dto.FileValidationResult;
import gov.nist.oar.common.datasetaccess.exception.DatasetAccessClientException;

import java.io.InputStream;
import java.util.List;

/**
 * Client interface for interacting with the Dataset Access Service.
 * <p>
 * This is a framework-agnostic interface that provides methods for:
 * <ul>
 *   <li>Validating files exist and getting their sizes (for bundle planning)</li>
 *   <li>Getting file metadata (HEAD requests)</li>
 *   <li>Streaming file content (for bundle creation)</li>
 *   <li>Listing AIP bags for a dataset</li>
 * </ul>
 * <p>
 * The default implementation uses Spring Cloud OpenFeign with service discovery.
 *
 * @see gov.nist.oar.common.datasetaccess.impl.spring.SpringDatasetAccessClient
 */
public interface DatasetAccessClient {

    /**
     * Validate that a file exists and is accessible.
     * <p>
     * This performs a HEAD request to get file metadata without downloading content.
     * Used by bundle-plan service to validate files before creating a bundle plan.
     * <p>
     * Maps to: HEAD /ds/{datasetId}/{filepath}
     *
     * @param datasetId the dataset identifier
     * @param filepath the file path within the dataset
     * @return validation result with size and accessibility status
     */
    FileValidationResult validateFile(String datasetId, String filepath);

    /**
     * Validate multiple files in a single batch operation.
     * <p>
     * More efficient than calling validateFile() repeatedly.
     *
     * @param datasetId the dataset identifier
     * @param filepaths list of file paths to validate
     * @return list of validation results, one per file
     */
    List<FileValidationResult> validateFiles(String datasetId, List<String> filepaths);

    /**
     * Get file metadata (size, content-type, checksum).
     * <p>
     * Maps to: HEAD /ds/{datasetId}/{filepath}
     *
     * @param datasetId the dataset identifier
     * @param filepath the file path within the dataset
     * @return file info with metadata
     * @throws DatasetAccessClientException if the file is not found or service error
     */
    FileInfo getFileInfo(String datasetId, String filepath) throws DatasetAccessClientException;

    /**
     * Get file metadata for a specific version of the dataset.
     * <p>
     * Maps to: HEAD /ds/{datasetId}/_v/{version}/{filepath}
     *
     * @param datasetId the dataset identifier
     * @param filepath the file path within the dataset
     * @param version the version string (or "latest")
     * @return file info with metadata
     * @throws DatasetAccessClientException if the file is not found or service error
     */
    FileInfo getFileInfo(String datasetId, String filepath, String version) throws DatasetAccessClientException;

    /**
     * Open an input stream to download a file.
     * <p>
     * The caller is responsible for closing the returned stream.
     * <p>
     * Maps to: GET /ds/{datasetId}/{filepath}
     *
     * @param datasetId the dataset identifier
     * @param filepath the file path within the dataset
     * @return input stream for reading file content
     * @throws DatasetAccessClientException if the file is not found or service error
     */
    InputStream openFileStream(String datasetId, String filepath) throws DatasetAccessClientException;

    /**
     * Open an input stream to download a specific version of a file.
     * <p>
     * Maps to: GET /ds/{datasetId}/_v/{version}/{filepath}
     *
     * @param datasetId the dataset identifier
     * @param filepath the file path within the dataset
     * @param version the version string (or "latest")
     * @return input stream for reading file content
     * @throws DatasetAccessClientException if the file is not found or service error
     */
    InputStream openFileStream(String datasetId, String filepath, String version) throws DatasetAccessClientException;

    /**
     * List all AIP (preservation bag) files for a dataset.
     * <p>
     * Maps to: GET /ds/{datasetId}/_aip
     *
     * @param datasetId the dataset identifier
     * @return list of AIP file descriptions
     * @throws DatasetAccessClientException if the dataset is not found or service error
     */
    List<FileInfo> listAIPs(String datasetId) throws DatasetAccessClientException;

    /**
     * List all versions available for a dataset.
     * <p>
     * Maps to: GET /ds/{datasetId}/_aip/_v
     *
     * @param datasetId the dataset identifier
     * @return list of version strings
     * @throws DatasetAccessClientException if the dataset is not found or service error
     */
    List<String> listVersions(String datasetId) throws DatasetAccessClientException;

    /**
     * Check if the dataset-access service is available.
     *
     * @return true if the service is reachable and healthy
     */
    boolean isAvailable();
}
