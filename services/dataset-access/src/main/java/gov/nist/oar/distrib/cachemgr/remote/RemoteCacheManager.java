package gov.nist.oar.distrib.cachemgr.remote;

import gov.nist.oar.common.cache.client.CacheManagerClient;
import gov.nist.oar.common.cache.dto.CacheObjectInfo;
import gov.nist.oar.common.cache.exception.CacheClientException;
import gov.nist.oar.distrib.cachemgr.CacheManager;
import gov.nist.oar.distrib.cachemgr.CacheManagementException;
import gov.nist.oar.distrib.cachemgr.CacheObject;
import gov.nist.oar.distrib.cachemgr.RestorationTargetNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Optional;

/**
 * A CacheManager implementation that delegates to a remote cache-mgmt microservice
 * via the CacheManagerClient (Feign-based client).
 * <p>
 * This adapter translates the local CacheManager API to remote microservice calls,
 * enabling the existing service layer to work with the distributed cache architecture.
 * <p>
 * Object IDs are expected to be in the format: "{datasetId}/{filepath}"
 * which is split and passed to the remote service.
 */
@Component
public class RemoteCacheManager extends CacheManager {

    private static final Logger logger = LoggerFactory.getLogger(RemoteCacheManager.class);

    @Autowired
    private CacheManagerClient cacheClient;

    @Value("${spring.application.name:dataset-access-service}")
    private String instanceId;

    /**
     * Parse object ID into datasetId and filepath components.
     * Expected format: "datasetId/filepath" or "datasetId/path/to/file.txt"
     *
     * @param id The object identifier
     * @return String array [datasetId, filepath]
     * @throws CacheManagementException if id format is invalid
     */
    private String[] parseId(String id) throws CacheManagementException {
        if (id == null || id.trim().isEmpty()) {
            throw new CacheManagementException("Object ID cannot be null or empty");
        }

        int firstSlash = id.indexOf('/');
        if (firstSlash <= 0 || firstSlash >= id.length() - 1) {
            throw new CacheManagementException(
                "Invalid object ID format: '" + id + "'. Expected format: 'datasetId/filepath'"
            );
        }

        String datasetId = id.substring(0, firstSlash);
        String filepath = id.substring(firstSlash + 1);

        return new String[] { datasetId, filepath };
    }

    /**
     * Check if an object is currently cached
     *
     * @param id The object identifier (datasetId/filepath)
     * @return true if the object is in the cache
     * @throws CacheManagementException if the check fails
     */
    @Override
    public boolean isCached(String id) throws CacheManagementException {
        try {
            String[] parts = parseId(id);
            String datasetId = parts[0];
            String filepath = parts[1];

            logger.debug("[{}] Checking cache for: {}/{}", instanceId, datasetId, filepath);

            boolean cached = cacheClient.isCached(datasetId, filepath);

            logger.debug("[{}] Cache check result for {}: {}", instanceId, id,
                        cached ? "HIT" : "MISS");

            return cached;

        } catch (CacheClientException e) {
            logger.error("[{}] Remote cache check failed for: {}", instanceId, id, e);
            throw new CacheManagementException("Failed to check cache for: " + id, e);
        }
    }

    /**
     * Cache an object (restore it from preservation to cache).
     * <p>
     * This method queues the object for caching in the remote cache-mgmt service.
     * The actual caching is asynchronous and may not be immediate.
     *
     * @param id The object identifier (datasetId/filepath)
     * @param prefs Cache preferences (currently ignored)
     * @param recache Whether to force recaching if already cached
     * @return true if object was queued for caching, false if already cached
     * @throws CacheManagementException if caching fails
     */
    @Override
    public boolean cache(String id, int prefs, boolean recache) throws CacheManagementException {
        try {
            String[] parts = parseId(id);
            String datasetId = parts[0];
            String filepath = parts[1];

            logger.debug("[{}] Cache request for: {}/{} (recache={})",
                        instanceId, datasetId, filepath, recache);

            // Check if already cached
            boolean alreadyCached = isCached(id);

            if (alreadyCached && !recache) {
                logger.debug("[{}] Object already cached: {}", instanceId, id);
                return false;
            }

            // Queue object for caching via remote API
            cacheClient.queueForCaching(datasetId, filepath, recache);
            logger.info("[{}] Queued object for caching: {}", instanceId, id);

            return true;

        } catch (CacheClientException e) {
            logger.error("[{}] Remote cache operation failed for: {}", instanceId, id, e);
            throw new CacheManagementException("Failed to cache: " + id, e);
        }
    }

    /**
     * Remove an object from the cache
     *
     * @param id The object identifier (datasetId/filepath)
     * @throws CacheManagementException if uncaching fails
     */
    @Override
    public void uncache(String id) throws CacheManagementException {
        try {
            String[] parts = parseId(id);
            String datasetId = parts[0];
            String filepath = parts[1];

            logger.debug("[{}] Uncache request for: {}/{}", instanceId, datasetId, filepath);

            cacheClient.removeFromCache(datasetId, filepath);
            logger.info("[{}] Removed object from cache: {}", instanceId, id);

        } catch (CacheClientException e) {
            logger.error("[{}] Remote uncache operation failed for: {}", instanceId, id, e);
            throw new CacheManagementException("Failed to uncache: " + id, e);
        }
    }

    /**
     * Find and return a CacheObject for the given identifier
     *
     * @param id The object identifier (datasetId/filepath)
     * @return CacheObject containing location and metadata
     * @throws CacheManagementException if the lookup fails
     */
    @Override
    public CacheObject findObject(String id) throws CacheManagementException {
        try {
            String[] parts = parseId(id);
            String datasetId = parts[0];
            String filepath = parts[1];

            logger.debug("[{}] Finding cache object for: {}/{}", instanceId, datasetId, filepath);

            Optional<CacheObjectInfo> infoOpt = cacheClient.getObjectInfo(datasetId, filepath);

            if (infoOpt.isPresent()) {
                CacheObjectInfo info = infoOpt.get();
                boolean isCached = Boolean.TRUE.equals(info.getCached());
                String volname = info.getVolumeName();

                // Use constructor that properly initializes _md
                CacheObject obj = new CacheObject(filepath, volname);
                obj.id = id;
                obj.cached = isCached;

                if (isCached) {
                    logger.debug("[{}] Found cached object: {} (volume: {}, size: {} bytes)",
                                instanceId, id, volname, info.getSize());
                } else {
                    logger.debug("[{}] Object exists in index but not cached: {}", instanceId, id);
                }
                return obj;
            } else {
                logger.debug("[{}] Object not found in cache: {}", instanceId, id);
                return null;  // Return null when not found, not an empty object
            }

        } catch (CacheClientException e) {
            logger.error("[{}] Remote findObject failed for: {}", instanceId, id, e);
            throw new CacheManagementException("Failed to find object: " + id, e);
        }
    }

    /**
     * Confirm that a cached object is still accessible
     *
     * @param obj The CacheObject to confirm
     * @return true if the object is still accessible
     * @throws CacheManagementException if the confirmation fails
     */
    @Override
    public boolean confirmAccessOf(CacheObject obj) throws CacheManagementException {
        if (obj == null || obj.id == null) {
            return false;
        }

        try {
            logger.debug("[{}] Confirming access for: {}", instanceId, obj.id);
            return isCached(obj.id);
        } catch (Exception e) {
            logger.error("[{}] Failed to confirm access for: {}", instanceId, obj.id, e);
            return false;
        }
    }

    /**
     * Get a redirect URL for accessing the cached object.
     * <p>
     * In the microservices architecture, we don't provide direct redirect URLs.
     * Instead, files are streamed through this service which internally fetches
     * from cache-mgmt. This provides better encapsulation and works in all
     * network configurations.
     *
     * @param id The object identifier (datasetId/filepath)
     * @return Always returns null - files are streamed through this service
     * @throws CacheManagementException if the operation fails
     */
    @Override
    public URL getRedirectFor(String id) throws CacheManagementException {
        // In microservices mode, we don't redirect - we stream through this service
        // The service layer will handle fetching from cache-mgmt and streaming to client
        logger.debug("[{}] No redirect URL for microservices mode: {}", instanceId, id);
        return null;
    }
}
