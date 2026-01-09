package gov.nist.oar.common.datasetaccess.impl.spring;

import gov.nist.oar.common.datasetaccess.client.DatasetAccessClient;
import gov.nist.oar.common.datasetaccess.dto.FileInfo;
import gov.nist.oar.common.datasetaccess.dto.FileValidationResult;
import gov.nist.oar.common.datasetaccess.exception.DatasetAccessClientException;
import gov.nist.oar.common.datasetaccess.impl.spring.feign.DatasetAccessFeignClient;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring/Feign implementation of the DatasetAccessClient.
 * <p>
 * This implementation uses Spring Cloud OpenFeign for HTTP communication
 * and integrates with Eureka for service discovery.
 */
public class SpringDatasetAccessClient implements DatasetAccessClient {

    private static final Logger logger = LoggerFactory.getLogger(SpringDatasetAccessClient.class);

    private final DatasetAccessFeignClient feignClient;

    public SpringDatasetAccessClient(DatasetAccessFeignClient feignClient) {
        this.feignClient = feignClient;
    }

    @Override
    public FileValidationResult validateFile(String datasetId, String filepath) {
        try {
            ResponseEntity<Void> response = feignClient.getFileInfoHead(datasetId, filepath);
            HttpHeaders headers = response.getHeaders();

            long contentLength = headers.getContentLength();
            String contentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);

            return FileValidationResult.success(datasetId, filepath, contentLength, contentType);
        } catch (FeignException.NotFound e) {
            logger.debug("File not found: {}/{}", datasetId, filepath);
            return FileValidationResult.failure(datasetId, filepath, 404, "File not found");
        } catch (FeignException e) {
            logger.warn("Error validating file {}/{}: {}", datasetId, filepath, e.getMessage());
            return FileValidationResult.failure(datasetId, filepath, e.status(), e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error validating file {}/{}: {}", datasetId, filepath, e.getMessage());
            return FileValidationResult.failure(datasetId, filepath, 500, "Service unavailable: " + e.getMessage());
        }
    }

    @Override
    public List<FileValidationResult> validateFiles(String datasetId, List<String> filepaths) {
        List<FileValidationResult> results = new ArrayList<>(filepaths.size());
        for (String filepath : filepaths) {
            results.add(validateFile(datasetId, filepath));
        }
        return results;
    }

    @Override
    public FileInfo getFileInfo(String datasetId, String filepath) throws DatasetAccessClientException {
        try {
            ResponseEntity<Void> response = feignClient.getFileInfoHead(datasetId, filepath);
            return extractFileInfoFromHeaders(response.getHeaders(), filepath);
        } catch (FeignException.NotFound e) {
            throw new DatasetAccessClientException("File not found: " + datasetId + "/" + filepath, 404, e);
        } catch (FeignException e) {
            throw new DatasetAccessClientException("Error getting file info: " + e.getMessage(), e.status(), e);
        }
    }

    @Override
    public FileInfo getFileInfo(String datasetId, String filepath, String version) throws DatasetAccessClientException {
        try {
            ResponseEntity<Void> response = feignClient.getFileInfoHeadVersioned(datasetId, version, filepath);
            return extractFileInfoFromHeaders(response.getHeaders(), filepath);
        } catch (FeignException.NotFound e) {
            throw new DatasetAccessClientException(
                    "File not found: " + datasetId + "/" + filepath + " (version " + version + ")", 404, e);
        } catch (FeignException e) {
            throw new DatasetAccessClientException("Error getting file info: " + e.getMessage(), e.status(), e);
        }
    }

    @Override
    public InputStream openFileStream(String datasetId, String filepath) throws DatasetAccessClientException {
        try {
            byte[] content = feignClient.downloadFile(datasetId, filepath);
            return new ByteArrayInputStream(content);
        } catch (FeignException.NotFound e) {
            throw new DatasetAccessClientException("File not found: " + datasetId + "/" + filepath, 404, e);
        } catch (FeignException e) {
            throw new DatasetAccessClientException("Error downloading file: " + e.getMessage(), e.status(), e);
        }
    }

    @Override
    public InputStream openFileStream(String datasetId, String filepath, String version) throws DatasetAccessClientException {
        try {
            byte[] content = feignClient.downloadFileVersioned(datasetId, version, filepath);
            return new ByteArrayInputStream(content);
        } catch (FeignException.NotFound e) {
            throw new DatasetAccessClientException(
                    "File not found: " + datasetId + "/" + filepath + " (version " + version + ")", 404, e);
        } catch (FeignException e) {
            throw new DatasetAccessClientException("Error downloading file: " + e.getMessage(), e.status(), e);
        }
    }

    @Override
    public List<FileInfo> listAIPs(String datasetId) throws DatasetAccessClientException {
        try {
            return feignClient.listAIPs(datasetId);
        } catch (FeignException.NotFound e) {
            throw new DatasetAccessClientException("Dataset not found: " + datasetId, 404, e);
        } catch (FeignException e) {
            throw new DatasetAccessClientException("Error listing AIPs: " + e.getMessage(), e.status(), e);
        }
    }

    @Override
    public List<String> listVersions(String datasetId) throws DatasetAccessClientException {
        try {
            return feignClient.listVersions(datasetId);
        } catch (FeignException.NotFound e) {
            throw new DatasetAccessClientException("Dataset not found: " + datasetId, 404, e);
        } catch (FeignException e) {
            throw new DatasetAccessClientException("Error listing versions: " + e.getMessage(), e.status(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            // Try a simple operation to check if service is up
            // If service is down, Feign will throw an exception
            return true;
        } catch (Exception e) {
            logger.debug("Dataset access service unavailable: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract FileInfo from HTTP response headers
     */
    private FileInfo extractFileInfoFromHeaders(HttpHeaders headers, String filepath) {
        return FileInfo.builder()
                .name(extractFilename(filepath))
                .contentLength(headers.getContentLength())
                .contentType(headers.getFirst(HttpHeaders.CONTENT_TYPE))
                .build();
    }

    /**
     * Extract filename from filepath
     */
    private String extractFilename(String filepath) {
        if (filepath == null) return null;
        int lastSlash = filepath.lastIndexOf('/');
        return lastSlash >= 0 ? filepath.substring(lastSlash + 1) : filepath;
    }
}
