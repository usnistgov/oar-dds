package gov.nist.oar.common.cache.impl.spring;

import gov.nist.oar.common.cache.client.CacheManagerClient;
import gov.nist.oar.common.cache.dto.CacheObjectInfo;
import gov.nist.oar.common.cache.dto.CacheObjectSummary;
import gov.nist.oar.common.cache.exception.CacheClientException;
import gov.nist.oar.common.cache.exception.CacheServiceUnavailableException;
import gov.nist.oar.common.cache.impl.spring.feign.CacheManagementFeignClient;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Spring-based implementation of CacheManagerClient using Feign.
 * <p>
 * This implementation:
 * - Uses Spring Cloud OpenFeign for HTTP communication
 * - Leverages Eureka for service discovery
 * - Applies circuit breakers via Resilience4j (if configured)
 * - Handles errors and translates them to domain exceptions
 * <p>
 * The Feign client provides automatic:
 * - JSON serialization/deserialization
 * - Load balancing across service instances
 * - Retries on failure
 * - Timeout handling
 */
public class SpringCacheManagerClient implements CacheManagerClient {

    private static final Logger logger = LoggerFactory.getLogger(SpringCacheManagerClient.class);

    private final CacheManagementFeignClient feignClient;

    /**
     * Create a new Spring-based cache manager client.
     *
     * @param feignClient the Feign client for HTTP communication
     */
    public SpringCacheManagerClient(CacheManagementFeignClient feignClient) {
        this.feignClient = feignClient;
    }

    @Override
    public Optional<CacheObjectInfo> getObjectInfo(String datasetId, String filepath)
            throws CacheClientException {
        try {
            logger.debug("Getting cache info for object: {}/{}", datasetId, filepath);
            CacheObjectInfo info = feignClient.getObjectInfo(datasetId, filepath);
            return Optional.ofNullable(info);
        } catch (FeignException.NotFound e) {
            logger.debug("Object not found in cache: {}/{}", datasetId, filepath);
            return Optional.empty();
        } catch (FeignException.FeignServerException e) {
            logger.error("Cache service unavailable for object: {}/{}", datasetId, filepath, e);
            throw new CacheServiceUnavailableException(
                    "Cache management service is unavailable", e);
        } catch (FeignException e) {
            logger.error("Error getting cache info for object: {}/{}", datasetId, filepath, e);
            throw new CacheClientException(
                    "Failed to get cache info for object: " + datasetId + "/" + filepath,
                    e, e.status(), datasetId + "/" + filepath);
        } catch (Exception e) {
            logger.error("Unexpected error getting cache info for object: {}/{}", datasetId, filepath, e);
            throw new CacheClientException(
                    "Unexpected error getting cache info for object: " + datasetId + "/" + filepath, e);
        }
    }

    @Override
    public boolean isCached(String datasetId, String filepath) throws CacheClientException {
        Optional<CacheObjectInfo> info = getObjectInfo(datasetId, filepath);
        return info.isPresent() && info.get().isAvailable();
    }

    @Override
    public CacheObjectSummary getDatasetSummary(String datasetId) throws CacheClientException {
        try {
            logger.debug("Getting dataset summary for: {}", datasetId);
            CacheObjectSummary summary = feignClient.getDatasetSummary(datasetId);
            logger.debug("Dataset summary for {}: {} files", datasetId,
                    summary.getFiles() != null ? summary.getFiles().size() : 0);
            return summary;
        } catch (FeignException.NotFound e) {
            logger.debug("Dataset not found: {}", datasetId);
            throw new CacheClientException("Dataset not found: " + datasetId, e, 404, datasetId);
        } catch (FeignException.FeignServerException e) {
            logger.error("Cache service unavailable for dataset: {}", datasetId, e);
            throw new CacheServiceUnavailableException(
                    "Cache management service is unavailable", e);
        } catch (FeignException e) {
            logger.error("Error getting dataset summary: {}", datasetId, e);
            throw new CacheClientException(
                    "Failed to get dataset summary: " + datasetId, e, e.status(), datasetId);
        } catch (Exception e) {
            logger.error("Unexpected error getting dataset summary: {}", datasetId, e);
            throw new CacheClientException(
                    "Unexpected error getting dataset summary: " + datasetId, e);
        }
    }

    @Override
    public void queueForCaching(String datasetId, String filepath, boolean recache)
            throws CacheClientException {
        try {
            logger.info("Queueing object for caching: {}/{} (recache={})",
                    datasetId, filepath, recache);
            String response = feignClient.queueForCaching(datasetId, filepath,
                    recache ? Boolean.TRUE : null);
            logger.info("Cache queue result for {}/{}: {}", datasetId, filepath, response);
        } catch (FeignException.NotFound e) {
            logger.warn("Object not found for caching: {}/{}", datasetId, filepath);
            throw new CacheClientException("Object not found: " + datasetId + "/" + filepath,
                    e, 404, datasetId + "/" + filepath);
        } catch (FeignException.FeignServerException e) {
            logger.error("Cache service unavailable when queueing object: {}/{}",
                    datasetId, filepath, e);
            throw new CacheServiceUnavailableException(
                    "Cache management service is unavailable", e);
        } catch (FeignException e) {
            logger.error("Error queueing object for caching: {}/{}", datasetId, filepath, e);
            throw new CacheClientException(
                    "Failed to queue object for caching: " + datasetId + "/" + filepath,
                    e, e.status(), datasetId + "/" + filepath);
        } catch (Exception e) {
            logger.error("Unexpected error queueing object for caching: {}/{}",
                    datasetId, filepath, e);
            throw new CacheClientException(
                    "Unexpected error queueing object for caching: " + datasetId + "/" + filepath, e);
        }
    }

    @Override
    public void removeFromCache(String datasetId, String filepath) throws CacheClientException {
        try {
            logger.info("Removing object from cache: {}/{}", datasetId, filepath);
            String response = feignClient.removeFromCache(datasetId, filepath);
            logger.info("Cache remove result for {}/{}: {}", datasetId, filepath, response);
        } catch (FeignException.NotFound e) {
            logger.debug("Object not found in cache for removal: {}/{}", datasetId, filepath);
            // Not throwing exception - it's OK if the object isn't cached
        } catch (FeignException.FeignServerException e) {
            logger.error("Cache service unavailable when removing object: {}/{}",
                    datasetId, filepath, e);
            throw new CacheServiceUnavailableException(
                    "Cache management service is unavailable", e);
        } catch (FeignException e) {
            logger.error("Error removing object from cache: {}/{}", datasetId, filepath, e);
            throw new CacheClientException(
                    "Failed to remove object from cache: " + datasetId + "/" + filepath,
                    e, e.status(), datasetId + "/" + filepath);
        } catch (Exception e) {
            logger.error("Unexpected error removing object from cache: {}/{}",
                    datasetId, filepath, e);
            throw new CacheClientException(
                    "Unexpected error removing object from cache: " + datasetId + "/" + filepath, e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            feignClient.getStatus();
            logger.debug("Cache service availability check: true");
            return true;
        } catch (Exception e) {
            logger.warn("Cache service is not available: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public java.util.Map<String, Object> getResourceMetadata(String datasetId)
            throws CacheClientException {
        try {
            logger.debug("Getting resource metadata for dataset: {}", datasetId);
            java.util.Map<String, Object> metadata = feignClient.getResourceMetadata(datasetId);
            logger.debug("Retrieved resource metadata for: {}", datasetId);
            return metadata;
        } catch (FeignException.NotFound e) {
            logger.warn("Dataset not found: {}", datasetId);
            throw new CacheClientException("Dataset not found: " + datasetId, e, 404, datasetId);
        } catch (FeignException.FeignServerException e) {
            logger.error("Cache service unavailable when getting resource metadata for: {}",
                    datasetId, e);
            throw new CacheServiceUnavailableException(
                    "Cache management service is unavailable", e);
        } catch (FeignException e) {
            logger.error("Error getting resource metadata for: {}", datasetId, e);
            throw new CacheClientException(
                    "Failed to get resource metadata for: " + datasetId,
                    e, e.status(), datasetId);
        } catch (Exception e) {
            logger.error("Unexpected error getting resource metadata for: {}", datasetId, e);
            throw new CacheClientException(
                    "Unexpected error getting resource metadata for: " + datasetId, e);
        }
    }

    @Override
    public java.util.Map<String, Object> getComponentMetadata(String datasetId, String filepath)
            throws CacheClientException {
        try {
            logger.debug("Getting component metadata for: {}/{}", datasetId, filepath);
            java.util.Map<String, Object> metadata =
                    feignClient.getComponentMetadata(datasetId, filepath);
            logger.debug("Retrieved component metadata for: {}/{}", datasetId, filepath);
            return metadata;
        } catch (FeignException.NotFound e) {
            logger.warn("File component not found: {}/{}", datasetId, filepath);
            throw new CacheClientException("File component not found: " + datasetId + "/" + filepath,
                    e, 404, datasetId + "/" + filepath);
        } catch (FeignException.FeignServerException e) {
            logger.error("Cache service unavailable when getting component metadata for: {}/{}",
                    datasetId, filepath, e);
            throw new CacheServiceUnavailableException(
                    "Cache management service is unavailable", e);
        } catch (FeignException e) {
            logger.error("Error getting component metadata for: {}/{}", datasetId, filepath, e);
            throw new CacheClientException(
                    "Failed to get component metadata for: " + datasetId + "/" + filepath,
                    e, e.status(), datasetId + "/" + filepath);
        } catch (Exception e) {
            logger.error("Unexpected error getting component metadata for: {}/{}",
                    datasetId, filepath, e);
            throw new CacheClientException(
                    "Unexpected error getting component metadata for: " + datasetId + "/" + filepath, e);
        }
    }

    // =====================================
    // RPA (Restricted Public Access) Methods
    // =====================================

    @Override
    public java.util.Map<String, Object> cacheDatasetForRPA(String datasetId, String version)
            throws CacheClientException {
        try {
            logger.info("Caching dataset for RPA: {} (version: {})", datasetId, version);
            java.util.Map<String, Object> result = feignClient.cacheDatasetForRPA(datasetId, version);
            logger.info("Dataset cached for RPA, randomId: {}", result.get("randomId"));
            return result;
        } catch (FeignException.NotFound e) {
            logger.warn("Dataset not found for RPA caching: {}", datasetId);
            throw new CacheClientException("Dataset not found: " + datasetId, e, 404, datasetId);
        } catch (FeignException.BadRequest e) {
            logger.warn("Bad request for RPA caching: {}", datasetId);
            throw new CacheClientException("Bad request: " + datasetId, e, 400, datasetId);
        } catch (FeignException.FeignServerException e) {
            logger.error("Cache service unavailable when caching dataset for RPA: {}", datasetId, e);
            throw new CacheServiceUnavailableException(
                    "Cache management service is unavailable", e);
        } catch (FeignException e) {
            logger.error("Error caching dataset for RPA: {}", datasetId, e);
            throw new CacheClientException(
                    "Failed to cache dataset for RPA: " + datasetId,
                    e, e.status(), datasetId);
        } catch (Exception e) {
            logger.error("Unexpected error caching dataset for RPA: {}", datasetId, e);
            throw new CacheClientException(
                    "Unexpected error caching dataset for RPA: " + datasetId, e);
        }
    }

    @Override
    public java.util.Map<String, Object> getRPACachedObjects(String randomId)
            throws CacheClientException {
        try {
            logger.debug("Getting RPA cached objects for randomId: {}", randomId);
            java.util.Map<String, Object> result = feignClient.getRPACachedObjects(randomId);
            logger.debug("Retrieved {} objects for RPA ID: {}", result.get("objectCount"), randomId);
            return result;
        } catch (FeignException.NotFound e) {
            logger.warn("RPA cached objects not found for randomId: {}", randomId);
            throw new CacheClientException("RPA objects not found: " + randomId, e, 404, randomId);
        } catch (FeignException.FeignServerException e) {
            logger.error("Cache service unavailable when getting RPA cached objects: {}", randomId, e);
            throw new CacheServiceUnavailableException(
                    "Cache management service is unavailable", e);
        } catch (FeignException e) {
            logger.error("Error getting RPA cached objects: {}", randomId, e);
            throw new CacheClientException(
                    "Failed to get RPA cached objects: " + randomId,
                    e, e.status(), randomId);
        } catch (Exception e) {
            logger.error("Unexpected error getting RPA cached objects: {}", randomId, e);
            throw new CacheClientException(
                    "Unexpected error getting RPA cached objects: " + randomId, e);
        }
    }

    @Override
    public boolean uncacheRPA(String randomId) throws CacheClientException {
        try {
            logger.info("Uncaching RPA objects for randomId: {}", randomId);
            java.util.Map<String, Object> result = feignClient.uncacheRPA(randomId);
            boolean success = Boolean.TRUE.equals(result.get("success"));
            logger.info("Uncached RPA objects for {}: success={}", randomId, success);
            return success;
        } catch (FeignException.NotFound e) {
            logger.debug("No RPA cached objects found for randomId: {}", randomId);
            return false;
        } catch (FeignException.FeignServerException e) {
            logger.error("Cache service unavailable when uncaching RPA objects: {}", randomId, e);
            throw new CacheServiceUnavailableException(
                    "Cache management service is unavailable", e);
        } catch (FeignException e) {
            logger.error("Error uncaching RPA objects: {}", randomId, e);
            throw new CacheClientException(
                    "Failed to uncache RPA objects: " + randomId,
                    e, e.status(), randomId);
        } catch (Exception e) {
            logger.error("Unexpected error uncaching RPA objects: {}", randomId, e);
            throw new CacheClientException(
                    "Unexpected error uncaching RPA objects: " + randomId, e);
        }
    }
}
