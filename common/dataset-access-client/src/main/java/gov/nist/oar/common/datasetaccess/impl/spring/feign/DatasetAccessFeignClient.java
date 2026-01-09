package gov.nist.oar.common.datasetaccess.impl.spring.feign;

import gov.nist.oar.common.datasetaccess.dto.FileInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feign client interface for the Dataset Access Service.
 * <p>
 * This is a declarative HTTP client that Spring Cloud implements automatically.
 * The "dataset-access-service" name matches the spring.application.name
 * in the dataset-access service configuration.
 * <p>
 * This client maps to the /ds API provided by DatasetAccessController.
 */
@FeignClient(
        name = "dataset-access-service",
        path = "/ds"
)
public interface DatasetAccessFeignClient {

    /**
     * Get file information via HEAD request.
     * <p>
     * Maps to: HEAD /ds/{datasetId}/{filepath}
     *
     * @param datasetId the dataset identifier
     * @param filepath the file path within the dataset
     * @return ResponseEntity with headers containing file metadata
     */
    @RequestMapping(value = "/{datasetId}/{filepath}", method = RequestMethod.HEAD)
    ResponseEntity<Void> getFileInfoHead(@PathVariable("datasetId") String datasetId,
                                         @PathVariable("filepath") String filepath);

    /**
     * Get file information via HEAD request for a specific version.
     * <p>
     * Maps to: HEAD /ds/{datasetId}/_v/{version}/{filepath}
     *
     * @param datasetId the dataset identifier
     * @param version the version string
     * @param filepath the file path within the dataset
     * @return ResponseEntity with headers containing file metadata
     */
    @RequestMapping(value = "/{datasetId}/_v/{version}/{filepath}", method = RequestMethod.HEAD)
    ResponseEntity<Void> getFileInfoHeadVersioned(@PathVariable("datasetId") String datasetId,
                                                   @PathVariable("version") String version,
                                                   @PathVariable("filepath") String filepath);

    /**
     * Download a file (returns byte array for small files).
     * <p>
     * Maps to: GET /ds/{datasetId}/{filepath}
     * <p>
     * Note: For large files, use streaming via RestTemplate or WebClient.
     *
     * @param datasetId the dataset identifier
     * @param filepath the file path within the dataset
     * @return file content as byte array
     */
    @GetMapping("/{datasetId}/{filepath}")
    byte[] downloadFile(@PathVariable("datasetId") String datasetId,
                        @PathVariable("filepath") String filepath);

    /**
     * Download a file with specific version.
     * <p>
     * Maps to: GET /ds/{datasetId}/_v/{version}/{filepath}
     *
     * @param datasetId the dataset identifier
     * @param version the version string
     * @param filepath the file path within the dataset
     * @return file content as byte array
     */
    @GetMapping("/{datasetId}/_v/{version}/{filepath}")
    byte[] downloadFileVersioned(@PathVariable("datasetId") String datasetId,
                                  @PathVariable("version") String version,
                                  @PathVariable("filepath") String filepath);

    /**
     * List AIP files for a dataset.
     * <p>
     * Maps to: GET /ds/{datasetId}/_aip
     *
     * @param datasetId the dataset identifier
     * @return list of AIP file descriptions
     */
    @GetMapping("/{datasetId}/_aip")
    List<FileInfo> listAIPs(@PathVariable("datasetId") String datasetId);

    /**
     * List versions available for a dataset.
     * <p>
     * Maps to: GET /ds/{datasetId}/_aip/_v
     *
     * @param datasetId the dataset identifier
     * @return list of version strings
     */
    @GetMapping("/{datasetId}/_aip/_v")
    List<String> listVersions(@PathVariable("datasetId") String datasetId);
}
