package gov.nist.oar.common.cache.client;

import gov.nist.oar.common.cache.dto.CacheObjectInfo;
import gov.nist.oar.common.cache.dto.CacheObjectSummary;
import gov.nist.oar.common.cache.exception.CacheClientException;

import java.util.List;
import java.util.Optional;

/**
 * Client interface for interacting with the Cache Management Service.
 * <p>
 * This is a framework-agnostic interface that can be implemented using different
 * HTTP clients (Feign, RestTemplate, plain HttpClient, etc.).
 * <p>
 * The default implementation uses Spring Cloud OpenFeign with circuit breakers
 * and retries for resilience.
 * <p>
 * This client maps to the existing /cache API provided by CacheManagementController.
 *
 * @see gov.nist.oar.common.cache.impl.spring.SpringCacheManagerClient
 */
public interface CacheManagerClient {

    /**
     * Get information about an object in the cache.
     * <p>
     * Maps to: GET /cache/objects/{datasetId}/{filepath}
     *
     * @param datasetId the dataset identifier
     * @param filepath the file path within the dataset
     * @return Optional containing cache info if found, empty otherwise
     * @throws CacheClientException if the request fails
     */
    Optional<CacheObjectInfo> getObjectInfo(String datasetId, String filepath)
            throws CacheClientException;

    /**
     * Simple boolean check if an object is cached.
     * <p>
     * This is a convenience method that wraps {@link #getObjectInfo(String, String)}.
     *
     * @param datasetId the dataset identifier
     * @param filepath the file path within the dataset
     * @return true if the object is cached, false otherwise
     * @throws CacheClientException if the request fails
     */
    boolean isCached(String datasetId, String filepath) throws CacheClientException;

    /**
     * Get summary information about a dataset's cached files.
     * <p>
     * Maps to: GET /cache/objects/{datasetId}
     *
     * @param datasetId the dataset identifier
     * @return summary containing dataset info and list of files
     * @throws CacheClientException if the request fails
     */
    CacheObjectSummary getDatasetSummary(String datasetId) throws CacheClientException;

    /**
     * Request that an object be added to the cache queue.
     * <p>
     * Maps to: PUT /cache/objects/{datasetId}/{filepath}/:cached
     * <p>
     * This is an asynchronous operation - the object may not be immediately available.
     *
     * @param datasetId the dataset identifier
     * @param filepath the file path within the dataset
     * @param recache if true, recache even if already cached
     * @throws CacheClientException if the request fails
     */
    void queueForCaching(String datasetId, String filepath, boolean recache)
            throws CacheClientException;

    /**
     * Request that an object be removed from the cache.
     * <p>
     * Maps to: DELETE /cache/objects/{datasetId}/{filepath}/:cached
     *
     * @param datasetId the dataset identifier
     * @param filepath the file path within the dataset
     * @throws CacheClientException if the request fails
     */
    void removeFromCache(String datasetId, String filepath) throws CacheClientException;

    /**
     * Check if the cache management service is available.
     * <p>
     * Maps to: GET /cache/
     *
     * @return true if the service is reachable and healthy, false otherwise
     */
    boolean isAvailable();

    /**
     * Get NERDm resource metadata for a dataset.
     * <p>
     * This retrieves the complete NERDm (NIST Extended Resource Dublin-core Metadata)
     * resource record from the dataset's head bag. The metadata includes dataset-level
     * information such as title, description, authors, and the full list of components.
     * <p>
     * Maps to: GET /cache/metadata/{datasetId}
     *
     * @param datasetId the dataset identifier (AIP ID, e.g., "mds2-2106")
     * @return Map containing the NERDm resource metadata
     * @throws CacheClientException if the request fails or the dataset is not found
     */
    java.util.Map<String, Object> getResourceMetadata(String datasetId)
            throws CacheClientException;

    /**
     * Get NERDm component metadata for a specific file within a dataset.
     * <p>
     * This retrieves the NERDm component metadata (file-level metadata) from the
     * dataset's head bag. Component metadata includes file size, checksum, content type,
     * description, and other file-specific information.
     * <p>
     * Maps to: GET /cache/metadata/{datasetId}/{filepath}
     *
     * @param datasetId the dataset identifier (AIP ID, e.g., "mds2-2106")
     * @param filepath the file path within the dataset
     * @return Map containing the NERDm component metadata
     * @throws CacheClientException if the request fails, dataset is not found, or file is not found
     */
    java.util.Map<String, Object> getComponentMetadata(String datasetId, String filepath)
            throws CacheClientException;

    // =====================================
    // RPA (Restricted Public Access) Methods
    // =====================================

    /**
     * Cache an entire dataset for Restricted Public Access (RPA).
     * <p>
     * This caches all files from a dataset using the ROLE_RESTRICTED_DATA preference
     * and returns a random ID that can be used to retrieve the cached objects.
     * <p>
     * Maps to: PUT /cache/rpa/{datasetId}
     *
     * @param datasetId the dataset identifier (AIP ID, e.g., "mds2-2106")
     * @param version the version of the dataset to cache (null for latest)
     * @return Map containing the randomId and list of cached files
     * @throws CacheClientException if the request fails or dataset is not found
     */
    java.util.Map<String, Object> cacheDatasetForRPA(String datasetId, String version)
            throws CacheClientException;

    /**
     * Get cached objects for an RPA session by random ID.
     * <p>
     * This retrieves all cached objects associated with a given RPA random ID.
     * The objects include file metadata such as filepath, size, checksum, etc.
     * <p>
     * Maps to: GET /cache/rpa/objects/{randomId}
     *
     * @param randomId the random ID returned from cacheDatasetForRPA
     * @return Map containing the randomId and list of cached object metadata
     * @throws CacheClientException if the request fails
     */
    java.util.Map<String, Object> getRPACachedObjects(String randomId)
            throws CacheClientException;

    /**
     * Uncache all objects for an RPA session by random ID.
     * <p>
     * This removes all cached objects associated with a given RPA random ID.
     * <p>
     * Maps to: DELETE /cache/rpa/objects/{randomId}
     *
     * @param randomId the random ID returned from cacheDatasetForRPA
     * @return true if at least one object was uncached, false otherwise
     * @throws CacheClientException if the request fails
     */
    boolean uncacheRPA(String randomId) throws CacheClientException;
}
