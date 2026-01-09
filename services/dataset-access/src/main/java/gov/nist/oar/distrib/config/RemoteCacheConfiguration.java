package gov.nist.oar.distrib.config;

import gov.nist.oar.distrib.cachemgr.CacheManager;
import gov.nist.oar.distrib.cachemgr.remote.RemoteCacheManager;
import gov.nist.oar.distrib.cachemgr.remote.RemoteHeadBagManager;
import gov.nist.oar.distrib.service.CacheEnabledFileDownloadService;
import gov.nist.oar.distrib.service.FileDownloadService;
import gov.nist.oar.distrib.service.PreservationBagService;
import jakarta.activation.MimetypesFileTypeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for Remote Cache Management in Microservices Architecture.
 * <p>
 * This configuration is activated when distrib.cache.mode=remote, which indicates
 * that the application should use the remote cache-mgmt microservice instead of
 * local cache management.
 * <p>
 * The RemoteCacheManager delegates cache operations to the cache-mgmt service
 * via the cache-manager-client (Feign-based).
 */
@Configuration
@ConditionalOnProperty(name = "distrib.cache.mode", havingValue = "remote")
public class RemoteCacheConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RemoteCacheConfiguration.class);

    @Autowired
    private RemoteCacheManager remoteCacheManager;

    @Autowired
    private RemoteHeadBagManager remoteHeadBagManager;

    @Value("${distrib.cache.trigger:false}")
    private boolean triggerCache;

    /**
     * Override the default FileDownloadService bean to use RemoteCacheManager
     * and RemoteHeadBagManager.
     * <p>
     * This bean is marked as @Primary to take precedence over the bean defined
     * in DatasetAccessServiceApplication when distrib.cache.mode=remote.
     * <p>
     * Both cache managers delegate to the remote cache-mgmt microservice:
     * - RemoteCacheManager handles data file caching
     * - RemoteHeadBagManager handles metadata resolution via head bags
     *
     * @param bagService The preservation bag service for accessing long-term storage
     * @param mimemap The MIME type map for content type resolution
     * @return CacheEnabledFileDownloadService configured with remote managers
     */
    @Bean
    @Primary
    public FileDownloadService remoteFileDownloadService(
            PreservationBagService bagService,
            MimetypesFileTypeMap mimemap) {

        logger.info("Configuring FileDownloadService with RemoteCacheManager and RemoteHeadBagManager");
        logger.info("Cache trigger mode: {}", triggerCache);

        // Create CacheEnabledFileDownloadService with remote managers
        // Both delegate to the cache-mgmt microservice for all operations
        return new CacheEnabledFileDownloadService(
            bagService,
            remoteCacheManager,
            remoteHeadBagManager,
            triggerCache,
            mimemap
        );
    }

    /**
     * Log configuration details on startup
     */
    @Bean
    public RemoteCacheConfigurationInfo remoteCacheConfigInfo() {
        logger.info("═══════════════════════════════════════════════════════════");
        logger.info("  Remote Cache Configuration ACTIVE");
        logger.info("═══════════════════════════════════════════════════════════");
        logger.info("  Mode: Microservices (Remote Cache)");
        logger.info("  Data Cache Manager: RemoteCacheManager");
        logger.info("  Metadata Manager: RemoteHeadBagManager");
        logger.info("  Communication: via cache-manager-client (Feign)");
        logger.info("  Target Service: cache-mgmt-service (via Eureka)");
        logger.info("  Trigger Cache: {}", triggerCache);
        logger.info("═══════════════════════════════════════════════════════════");
        return new RemoteCacheConfigurationInfo();
    }

    /**
     * Simple info class for logging purposes
     */
    public static class RemoteCacheConfigurationInfo {
        // Marker class for configuration logging
    }
}
