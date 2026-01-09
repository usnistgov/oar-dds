package gov.nist.oar.distrib.cachemgr.remote;

import gov.nist.oar.distrib.ResourceNotFoundException;
import gov.nist.oar.distrib.cachemgr.CacheManagementException;
import gov.nist.oar.distrib.cachemgr.CacheObject;
import gov.nist.oar.distrib.cachemgr.pdr.HeadBagCacheManager;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;

/**
 * A minimal HeadBagCacheManager implementation for microservices mode that delegates
 * metadata resolution to the remote cache-mgmt service.
 * <p>
 * This implementation extends HeadBagCacheManager but does NOT manage local head bag
 * caching. Instead, it delegates all metadata operations to RemoteHeadBagAdapter which
 * calls the remote cache-mgmt service.
 * <p>
 * Methods that require local caching (like getObject()) throw UnsupportedOperationException.
 * For the microservices architecture, head bags are managed centrally by the cache-mgmt service.
 */
@Component
public class RemoteHeadBagManager extends HeadBagCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(RemoteHeadBagManager.class);

    @Autowired
    private RemoteHeadBagAdapter remoteAdapter;

    /**
     * Constructor that bypasses the need for local cache infrastructure.
     * <p>
     * In microservices mode, we don't need a local BasicCache, HeadBagDB, or HeadBagRestorer
     * because all head bag operations are delegated to the remote cache-mgmt service.
     */
    public RemoteHeadBagManager() {
        // Pass nulls to super constructor - we override all methods that would use these
        super(null, null, null, "88434");
        logger.info("RemoteHeadBagManager initialized for microservices mode");
    }

    /**
     * Return the NERDm resource record for the dataset with the given AIPID.
     * <p>
     * This delegates to the remote cache-mgmt service via RemoteHeadBagAdapter.
     *
     * @param aipid the AIP ID for the dataset
     * @param version the desired version (null for latest)
     * @return JSONObject containing the NERDm resource metadata
     * @throws ResourceNotFoundException if the dataset cannot be found
     * @throws CacheManagementException if an error occurs
     */
    @Override
    public JSONObject resolveAIPID(String aipid, String version)
            throws CacheManagementException, ResourceNotFoundException {
        logger.debug("Delegating resolveAIPID to remote adapter: {}", aipid);
        return remoteAdapter.resolveAIPID(aipid, version);
    }

    /**
     * Return a NERDm component metadata record for a specific file.
     * <p>
     * This delegates to the remote cache-mgmt service via RemoteHeadBagAdapter.
     *
     * @param aipid the AIP-ID of the dataset
     * @param filepath the filepath to the component
     * @param version the desired version (null for latest)
     * @return JSONObject containing the NERDm component metadata
     * @throws ResourceNotFoundException if the dataset cannot be found
     * @throws FileNotFoundException if the file is not found
     * @throws CacheManagementException if an error occurs
     */
    @Override
    public JSONObject resolveDistribution(String aipid, String filepath, String version)
            throws CacheManagementException, ResourceNotFoundException, FileNotFoundException {
        logger.debug("Delegating resolveDistribution to remote adapter: {}/{}", aipid, filepath);
        return remoteAdapter.resolveDistribution(aipid, filepath, version);
    }

    /**
     * Get a cached object from local storage.
     * <p>
     * NOT SUPPORTED in microservices mode. Head bags are managed centrally by cache-mgmt service.
     * If you need head bag access, use the remote metadata endpoints instead.
     *
     * @throws UnsupportedOperationException always - local head bag caching not supported
     */
    @Override
    public CacheObject getObject(String id) throws CacheManagementException {
        throw new UnsupportedOperationException(
                "Local head bag caching not supported in microservices mode. " +
                "Head bags are managed by the cache-mgmt service. " +
                "Use resolveAIPID() or resolveDistribution() for metadata instead.");
    }

    /**
     * Find an object in the cache.
     * <p>
     * NOT SUPPORTED in microservices mode - use remote metadata endpoints instead.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public CacheObject findObject(String id) throws CacheManagementException {
        throw new UnsupportedOperationException(
                "Local head bag caching not supported in microservices mode. " +
                "Use resolveAIPID() or resolveDistribution() for metadata instead.");
    }

    /**
     * Cache an object.
     * <p>
     * NOT SUPPORTED in microservices mode - head bags are managed by cache-mgmt service.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public boolean cache(String id, int prefs, boolean recache) throws CacheManagementException {
        throw new UnsupportedOperationException(
                "Local head bag caching not supported in microservices mode.");
    }

    /**
     * Check if an object is cached.
     * <p>
     * NOT SUPPORTED in microservices mode.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public boolean isCached(String id) throws CacheManagementException {
        throw new UnsupportedOperationException(
                "Local head bag caching not supported in microservices mode.");
    }

    /**
     * Remove an object from cache.
     * <p>
     * NOT SUPPORTED in microservices mode.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void uncache(String id) throws CacheManagementException {
        throw new UnsupportedOperationException(
                "Local head bag caching not supported in microservices mode.");
    }
}
