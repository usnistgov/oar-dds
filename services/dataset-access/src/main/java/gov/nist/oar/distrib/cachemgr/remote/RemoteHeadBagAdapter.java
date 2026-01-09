package gov.nist.oar.distrib.cachemgr.remote;

import gov.nist.oar.common.cache.client.CacheManagerClient;
import gov.nist.oar.common.cache.exception.CacheClientException;
import gov.nist.oar.distrib.ResourceNotFoundException;
import gov.nist.oar.distrib.cachemgr.CacheManagementException;
import gov.nist.oar.distrib.cachemgr.pdr.HeadBagCacheManager;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.util.Map;

/**
 * A remote adapter for HeadBagCacheManager that delegates metadata resolution
 * to the cache-mgmt microservice via CacheManagerClient.
 * <p>
 * This adapter provides a minimal implementation of HeadBagCacheManager that only
 * supports metadata resolution operations (resolveAIPID, resolveDistribution).
 * It does NOT support caching operations - those are handled by the remote service.
 * <p>
 * This enables CacheEnabledFileDownloadService to retrieve file metadata (size,
 * checksum, etc.) from the centralized cache-mgmt service without needing local
 * head bag storage.
 */
@Component
public class RemoteHeadBagAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RemoteHeadBagAdapter.class);

    @Autowired
    private CacheManagerClient cacheClient;

    @Value("${spring.application.name:dataset-access-service}")
    private String instanceId;

    /**
     * Return the NERDm resource record for the dataset with the given AIPID.
     * <p>
     * This method delegates to the remote cache-mgmt service to retrieve metadata
     * from the dataset's head bag.
     *
     * @param aipid the AIP ID for the dataset (e.g., "mds2-2106")
     * @param version the desired version (null or empty for latest)
     * @return JSONObject containing the NERDm resource metadata
     * @throws ResourceNotFoundException if the AIP cannot be located
     * @throws CacheManagementException if an error occurs retrieving metadata
     */
    public JSONObject resolveAIPID(String aipid, String version)
            throws CacheManagementException, ResourceNotFoundException {

        String aip = aipid;
        int p = aipid.indexOf("/");
        if (p >= 0)
            aipid = aipid.substring(0, p);
        if (aipid.length() == 0)
            throw new ResourceNotFoundException(aip);

        logger.debug("[{}] Resolving AIP ID: {} (version: {})", instanceId, aipid, version);

        try {
            // Call remote cache-mgmt service for metadata
            Map<String, Object> metadata = cacheClient.getResourceMetadata(aipid);

            // Convert Map to JSONObject
            JSONObject result = new JSONObject(metadata);

            logger.debug("[{}] Resolved AIP ID: {} -> {} components",
                    instanceId, aipid, result.optJSONArray("components").length());

            return result;

        } catch (CacheClientException e) {
            // Check if it's a 404 (not found)
            if (e.getMessage().contains("not found")) {
                logger.warn("[{}] AIP ID not found: {}", instanceId, aipid);
                throw new ResourceNotFoundException(aip);
            }

            logger.error("[{}] Error resolving AIP ID: {}", instanceId, aipid, e);
            throw new CacheManagementException("Failed to resolve AIP ID: " + aipid, e);
        }
    }

    /**
     * Return a NERDm component metadata record corresponding to the given AIP-ID and filepath.
     * <p>
     * This method delegates to the remote cache-mgmt service to retrieve component metadata
     * from the dataset's head bag.
     *
     * @param aipid the AIP-ID of the dataset (e.g., "mds2-2106")
     * @param filepath the filepath to the component (e.g., "trial1.json")
     * @param version the desired version (null or empty for latest)
     * @return JSONObject containing the NERDm component metadata
     * @throws ResourceNotFoundException if the dataset cannot be found
     * @throws FileNotFoundException if the file component is not found in the dataset
     * @throws CacheManagementException if an error occurs retrieving metadata
     */
    public JSONObject resolveDistribution(String aipid, String filepath, String version)
            throws CacheManagementException, ResourceNotFoundException, FileNotFoundException {

        logger.debug("[{}] Resolving distribution: {}/{} (version: {})",
                instanceId, aipid, filepath, version);

        try {
            // Call remote cache-mgmt service for component metadata
            Map<String, Object> metadata = cacheClient.getComponentMetadata(aipid, filepath);

            // Convert Map to JSONObject
            JSONObject result = new JSONObject(metadata);

            logger.debug("[{}] Resolved component: {}/{} -> size={} bytes",
                    instanceId, aipid, filepath, result.optLong("size", -1));

            return result;

        } catch (CacheClientException e) {
            // Check if it's a 404 for the file
            if (e.getMessage().contains("not found")) {
                if (e.getMessage().contains("Dataset not found")) {
                    logger.warn("[{}] Dataset not found: {}", instanceId, aipid);
                    throw new ResourceNotFoundException(aipid);
                } else {
                    logger.warn("[{}] File component not found: {}/{}", instanceId, aipid, filepath);
                    throw new FileNotFoundException(filepath + ": file component not found in " + aipid);
                }
            }

            logger.error("[{}] Error resolving distribution: {}/{}", instanceId, aipid, filepath, e);
            throw new CacheManagementException("Failed to resolve distribution: " + aipid + "/" + filepath, e);
        }
    }

    /**
     * Return the NAAN (Name Assigning Authority Number) for ARK identifiers.
     * <p>
     * For PDR, this is typically "88434".
     *
     * @return the NAAN string
     */
    public String getARKNAAN() {
        return "88434";
    }
}
