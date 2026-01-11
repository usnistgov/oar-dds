/**
 * This software was developed at the National Institute of Standards and Technology by employees of
 * the Federal Government in the course of their official duties. Pursuant to title 17 Section 105
 * of the United States Code this software is not subject to copyright protection and is in the
 * public domain. This is an experimental system. NIST assumes no responsibility whatsoever for its
 * use by other parties, and makes no guarantees, expressed or implied, about its quality,
 * reliability, or any other characteristic. We would appreciate acknowledgement if the software is
 * used. This software can be redistributed and/or modified freely provided that any derivative
 * works bear some notice that they are derived from it, and any modified versions bear some notice
 * that they have been modified.
 */
package gov.nist.oar.distrib.service;

import gov.nist.oar.common.cache.client.CacheManagerClient;
import gov.nist.oar.common.cache.dto.CacheObjectInfo;
import gov.nist.oar.common.cache.exception.CacheClientException;
import gov.nist.oar.distrib.DistributionException;
import gov.nist.oar.distrib.ResourceNotFoundException;
import gov.nist.oar.distrib.FileDescription;
import gov.nist.oar.distrib.StreamHandle;
import gov.nist.oar.distrib.Checksum;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import jakarta.activation.MimetypesFileTypeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A FileDownloadService implementation that delegates cache operations to a remote cache-mgmt service.
 * <p>
 * This is the microservices-compatible version of CacheEnabledFileDownloadService. Instead of managing
 * a local cache, it delegates all cache operations to the cache-mgmt microservice via a Feign client.
 * <p>
 * The service:
 * <ul>
 *   <li>Checks for cached files via the remote cache service</li>
 *   <li>Returns redirect URLs for cached files when available</li>
 *   <li>Falls back to direct extraction from preservation bags when not cached</li>
 *   <li>Retrieves metadata from the remote cache service's head bag management</li>
 * </ul>
 */
public class RemoteCacheEnabledFileDownloadService implements FileDownloadService {

    private static final Logger logger = LoggerFactory.getLogger(RemoteCacheEnabledFileDownloadService.class);

    private final FileDownloadService fallbackService;
    private final CacheManagerClient cacheClient;
    private final MimetypesFileTypeMap typemap;
    private final boolean autocache;

    /**
     * Create the service instance.
     *
     * @param fallbackService  the service to use when files are not in cache (direct from bags)
     * @param cacheClient      the client for communicating with the remote cache-mgmt service
     * @param mimemap          the map to use for determining content types from filename extensions
     * @param triggercache     if true, requests for uncached files will trigger caching
     */
    public RemoteCacheEnabledFileDownloadService(FileDownloadService fallbackService,
                                                  CacheManagerClient cacheClient,
                                                  MimetypesFileTypeMap mimemap,
                                                  boolean triggercache) {
        this.fallbackService = fallbackService;
        this.cacheClient = cacheClient;
        this.typemap = mimemap != null ? mimemap : loadDefaultMimeTypes();
        this.autocache = triggercache;
    }

    private MimetypesFileTypeMap loadDefaultMimeTypes() {
        InputStream mis = getClass().getResourceAsStream("/mime.types");
        return (mis == null) ? new MimetypesFileTypeMap() : new MimetypesFileTypeMap(mis);
    }

    /**
     * Return a default content type based on the given file name.
     */
    public String getDefaultContentType(String filename) {
        return typemap.getContentType(filename);
    }

    /**
     * Describe the data file with the given filepath.  The returned information includes the
     * file size, type, and checksum information.
     */
    @Override
    public FileDescription getDataFileInfo(String dsid, String filepath, String version)
        throws ResourceNotFoundException, DistributionException, FileNotFoundException
    {
        // look for the file in the cache
        CacheObjectInfo co = findCachedObject(dsid, filepath, version);
        if (co != null)
            return cacheObjectInfoToFileDesc(co, filepath);

        // Try to get metadata from the cache service
        try {
            Map<String, Object> metadata = cacheClient.getComponentMetadata(dsid, filepath);
            if (metadata != null) {
                return metadataToFileDesc(metadata, filepath);
            }
        } catch (CacheClientException ex) {
            logger.debug("Metadata lookup failed for {}/{}: {}", dsid, filepath, ex.getMessage());
            // Fall through to fallback service
        }

        // Fall back to direct extraction from bags
        return fallbackService.getDataFileInfo(dsid, filepath, version);
    }

    /**
     * Download the data file with the given filepath.
     * <p>
     * The caller is responsible for closing the return stream.
     */
    @Override
    public StreamHandle getDataFile(String dsid, String filepath, String version)
        throws ResourceNotFoundException, DistributionException, FileNotFoundException
    {
        // check cache
        CacheObjectInfo co = findCachedObject(dsid, filepath, version);
        if (co != null) {
            // File is cached - caller should use getDataFileRedirect for redirect URLs
            // Here we still stream from fallback service (remote cache doesn't provide streams)
            logger.debug("{}/{}: File is cached, streaming from source", dsid, filepath);
        }

        // last resort: straight from long-term storage
        StreamHandle out = fallbackService.getDataFile(dsid, filepath, version);

        if (autocache) {
            try {
                // possibly cache the requested dataset for the next request
                cacheClient.queueForCaching(dsid, filepath, false);
            } catch (CacheClientException ex) {
                logger.warn("Failed to cache-queue dataset {}: {}", dsid, ex.getMessage());
            }
        }

        return out;
    }

    /**
     * return a URL where the identified file can be downloaded directly from.  This returns
     * a redirect URL if the file is cached with a public URL.
     */
    @Override
    public URL getDataFileRedirect(String dsid, String filepath, String version)
        throws ResourceNotFoundException, DistributionException, FileNotFoundException
    {
        // check cache
        return redirectFor(findCachedObject(dsid, filepath, version));
    }

    /**
     * Return the filepaths of data files available from the dataset.
     */
    public List<String> listDataFiles(String dsid, String version)
        throws ResourceNotFoundException, DistributionException
    {
        // Delegate to cache service's metadata endpoint
        try {
            Map<String, Object> metadata = cacheClient.getResourceMetadata(dsid);
            if (metadata != null && metadata.containsKey("components")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> components = (List<Map<String, Object>>) metadata.get("components");
                return components.stream()
                    .filter(c -> c.containsKey("filepath"))
                    .map(c -> (String) c.get("filepath"))
                    .toList();
            }
        } catch (CacheClientException ex) {
            logger.warn("Failed to list files via cache service for {}: {}", dsid, ex.getMessage());
        }

        throw new DistributionException("Unable to list data files for dataset: " + dsid);
    }

    /**
     * return CacheObjectInfo corresponding to a file in the cache matching the given identifiers.
     * @param dsid      the dataset identifier for the desired dataset
     * @param filepath  the path within the dataset to the desired file
     * @param version   the version of the dataset.  If null, the latest version is returned.
     * @return CacheObjectInfo if the file is available in the cache; null, otherwise.
     */
    public CacheObjectInfo findCachedObject(String dsid, String filepath, String version) {
        try {
            CacheObjectInfo out = cacheClient.getObjectInfo(dsid, filepath).orElse(null);
            if (out != null && !out.isCached()) {
                logger.debug("{}/{}: FYI: object found but not cached", dsid, filepath);
                out = null;
            }
            if (out != null)
                return out;
        }
        catch (CacheClientException ex) {
            logger.error("Failure while searching cache: {} (skipping)", ex.getMessage());
            logger.warn("Ignoring cached version due to error");
        }
        return null;
    }

    /**
     * given a {@link CacheObjectInfo}, return a redirect URL for accessing the underlying object
     * or null, if such a URL is not available.  Using this function on a CacheObjectInfo returned
     * by {@link #findCachedObject(String,String,String)} is more efficient than calling
     * {@link #getDataFileRedirect(String,String,String)} followed possibly by a call to
     * {@link #getDataFile(String,String,String)} as the latter will repeat the search of the cache.
     */
    public URL redirectFor(CacheObjectInfo co) {
        if (co == null || !co.isCached())
            return null;

        URL out = null;
        String redirectUrl = co.getRedirectUrl();
        if (redirectUrl != null && !redirectUrl.isEmpty()) {
            try {
                out = new URL(redirectUrl);
            } catch (Exception ex) {
                logger.error("Failure creating redirect URL: {} (skipping)", ex.getMessage());
                logger.warn("Ignoring redirect for cached version of file");
            }
        }

        return out;
    }

    private FileDescription cacheObjectInfoToFileDesc(CacheObjectInfo info, String filepath) {
        FileDescription fd = new FileDescription(filepath, info.getSize(), info.getContentType());
        if (info.getChecksum() != null) {
            fd.checksum = new Checksum(info.getChecksum(), info.getChecksumAlgorithm());
        }
        return fd;
    }

    private FileDescription metadataToFileDesc(Map<String, Object> metadata, String filepath) {
        long size = metadata.containsKey("size") ? ((Number) metadata.get("size")).longValue() : 0;
        String contentType = (String) metadata.get("mediaType");
        if (contentType == null) {
            contentType = getDefaultContentType(filepath);
        }

        FileDescription fd = new FileDescription(filepath, size, contentType);

        @SuppressWarnings("unchecked")
        Map<String, Object> checksumMap = (Map<String, Object>) metadata.get("checksum");
        if (checksumMap != null) {
            String hash = (String) checksumMap.get("hash");
            String algorithm = (String) checksumMap.get("algorithm");
            if (hash != null) {
                fd.checksum = new Checksum(hash, algorithm);
            }
        }

        return fd;
    }
}
