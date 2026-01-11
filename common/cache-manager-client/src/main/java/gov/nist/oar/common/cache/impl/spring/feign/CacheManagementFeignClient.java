package gov.nist.oar.common.cache.impl.spring.feign;

import gov.nist.oar.common.cache.dto.CacheObjectInfo;
import gov.nist.oar.common.cache.dto.CacheObjectSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Feign client interface for the Cache Management Service.
 * <p>
 * This is a declarative HTTP client that Spring Cloud will implement automatically.
 * The @FeignClient annotation tells Spring to create a proxy that:
 * - Uses Eureka for service discovery (via service name)
 * - Applies circuit breakers (if configured)
 * - Handles retries and timeouts
 * - Serializes/deserializes JSON automatically
 * <p>
 * The "cache-management-service" name matches the spring.application.name
 * in the cache-mgmt service configuration.
 * <p>
 * This client maps to the existing /cache API provided by CacheManagementController.
 */
@FeignClient(
        name = "cache-mgmt-service",
        path = "/cache"
)
public interface CacheManagementFeignClient {

    /**
     * Health check - check if cache manager is operational.
     * <p>
     * Maps to: GET /cache/
     *
     * @return status information
     */
    @GetMapping("/")
    Map<String, Object> getStatus();

    /**
     * Get information about an object in the cache.
     * <p>
     * Maps to: GET /cache/objects/{datasetId}/{filepath}
     * <p>
     * Note: The controller uses wildcard matching, so filepath can contain slashes.
     *
     * @param datasetId the dataset identifier
     * @param filepath the file path (can contain slashes)
     * @return cache object information
     */
    @GetMapping("/objects/{datasetId}/{filepath}")
    CacheObjectInfo getObjectInfo(@PathVariable("datasetId") String datasetId,
                                   @PathVariable("filepath") String filepath);

    /**
     * Get summary information about a dataset.
     * <p>
     * Maps to: GET /cache/objects/{datasetId}
     *
     * @param datasetId the dataset identifier
     * @return dataset summary with list of files
     */
    @GetMapping("/objects/{datasetId}")
    CacheObjectSummary getDatasetSummary(@PathVariable("datasetId") String datasetId);

    /**
     * Queue an object for caching.
     * <p>
     * Maps to: PUT /cache/objects/{datasetId}/{filepath}/:cached
     *
     * @param datasetId the dataset identifier
     * @param filepath the file path (can contain slashes)
     * @param recache if true, recache even if already cached
     * @return response message
     */
    @PutMapping("/objects/{datasetId}/{filepath}/:cached")
    String queueForCaching(@PathVariable("datasetId") String datasetId,
                           @PathVariable("filepath") String filepath,
                           @RequestParam(value = "recache", required = false) Boolean recache);

    /**
     * Remove an object from the cache.
     * <p>
     * Maps to: DELETE /cache/objects/{datasetId}/{filepath}/:cached
     *
     * @param datasetId the dataset identifier
     * @param filepath the file path (can contain slashes)
     * @return response message
     */
    @DeleteMapping("/objects/{datasetId}/{filepath}/:cached")
    String removeFromCache(@PathVariable("datasetId") String datasetId,
                           @PathVariable("filepath") String filepath);

    /**
     * Get NERDm resource metadata for a dataset.
     * <p>
     * Maps to: GET /cache/metadata/{datasetId}
     * <p>
     * This retrieves the complete NERDm (NIST Extended Resource Dublin-core Metadata)
     * resource record from the dataset's head bag.
     *
     * @param datasetId the dataset identifier (AIP ID, e.g., "mds2-2106")
     * @return Map containing the NERDm resource metadata
     */
    @GetMapping("/metadata/{datasetId}")
    Map<String, Object> getResourceMetadata(@PathVariable("datasetId") String datasetId);

    /**
     * Get NERDm component metadata for a specific file within a dataset.
     * <p>
     * Maps to: GET /cache/metadata/{datasetId}/{filepath}
     * <p>
     * This retrieves the NERDm component metadata (file-level metadata) from the
     * dataset's head bag.
     *
     * @param datasetId the dataset identifier (AIP ID, e.g., "mds2-2106")
     * @param filepath the file path within the dataset (can contain slashes)
     * @return Map containing the NERDm component metadata
     */
    @GetMapping("/metadata/{datasetId}/{filepath}")
    Map<String, Object> getComponentMetadata(@PathVariable("datasetId") String datasetId,
                                              @PathVariable("filepath") String filepath);

    // =====================================
    // RPA (Restricted Public Access) Methods
    // =====================================

    /**
     * Cache an entire dataset for Restricted Public Access (RPA).
     * <p>
     * Maps to: PUT /cache/rpa/{datasetId}
     * <p>
     * This caches all files from a dataset using the ROLE_RESTRICTED_DATA preference
     * and returns a random ID that can be used to retrieve the cached objects.
     *
     * @param datasetId the dataset identifier (AIP ID, e.g., "mds2-2106")
     * @param version the version of the dataset to cache (null for latest)
     * @return Map containing the randomId and list of cached files
     */
    @PutMapping("/rpa/{datasetId}")
    Map<String, Object> cacheDatasetForRPA(@PathVariable("datasetId") String datasetId,
                                            @RequestParam(value = "version", required = false) String version);

    /**
     * Get cached objects for an RPA session by random ID.
     * <p>
     * Maps to: GET /cache/rpa/objects/{randomId}
     * <p>
     * This retrieves all cached objects associated with a given RPA random ID.
     *
     * @param randomId the random ID returned from cacheDatasetForRPA
     * @return Map containing the randomId and list of cached object metadata
     */
    @GetMapping("/rpa/objects/{randomId}")
    Map<String, Object> getRPACachedObjects(@PathVariable("randomId") String randomId);

    /**
     * Uncache all objects for an RPA session by random ID.
     * <p>
     * Maps to: DELETE /cache/rpa/objects/{randomId}
     * <p>
     * This removes all cached objects associated with a given RPA random ID.
     *
     * @param randomId the random ID returned from cacheDatasetForRPA
     * @return Map containing uncache result information
     */
    @DeleteMapping("/rpa/objects/{randomId}")
    Map<String, Object> uncacheRPA(@PathVariable("randomId") String randomId);
}
