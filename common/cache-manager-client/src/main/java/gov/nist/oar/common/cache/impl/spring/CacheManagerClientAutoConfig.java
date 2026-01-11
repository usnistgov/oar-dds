package gov.nist.oar.common.cache.impl.spring;

import gov.nist.oar.common.cache.client.CacheManagerClient;
import gov.nist.oar.common.cache.dto.CacheObjectInfo;
import gov.nist.oar.common.cache.dto.CacheObjectSummary;
import gov.nist.oar.common.cache.impl.spring.feign.CacheManagementFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot auto-configuration for the Cache Manager Client.
 * <p>
 * This configuration is automatically activated when:
 * 1. Spring Cloud OpenFeign is on the classpath
 * 2. The application doesn't provide its own CacheManagerClient bean
 * <p>
 * To use this client library in a Spring Boot application:
 * 1. Add cache-manager-client as a dependency
 * 2. Ensure Spring Cloud OpenFeign is configured
 * 3. The CacheManagerClient bean will be automatically available
 * <p>
 * To customize configuration, add these properties to application.yml:
 * <pre>
 * cache-client:
 *   enabled: true  # Enable/disable the client
 *   connect-timeout: 5000  # Connection timeout in ms
 *   read-timeout: 10000    # Read timeout in ms
 * </pre>
 * <p>
 * This will be discovered via spring.factories (META-INF).
 */
@Configuration
@ConditionalOnClass({CacheManagerClient.class, CacheManagementFeignClient.class})
@EnableFeignClients(basePackageClasses = CacheManagementFeignClient.class)
@EnableConfigurationProperties(CacheManagerClientAutoConfig.CacheClientProperties.class)
public class CacheManagerClientAutoConfig {

    private static final Logger logger = LoggerFactory.getLogger(CacheManagerClientAutoConfig.class);

    /**
     * Create the CacheManagerClient bean if one doesn't already exist.
     *
     * @param feignClient the Feign client (auto-created by Spring)
     * @return a configured CacheManagerClient instance
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheManagerClient cacheManagerClient(CacheManagementFeignClient feignClient,
                                                   CacheClientProperties properties) {
        logger.info("Auto-configuring CacheManagerClient with Spring/Feign implementation");
        logger.debug("Cache client properties - enabled: {}, connectTimeout: {}, readTimeout: {}",
                properties.isEnabled(), properties.getConnectTimeout(), properties.getReadTimeout());

        if (!properties.isEnabled()) {
            logger.warn("Cache client is disabled via configuration");
            return new NoOpCacheManagerClient();
        }

        return new SpringCacheManagerClient(feignClient);
    }

    /**
     * Configuration properties for the cache client.
     */
    @ConfigurationProperties(prefix = "cache-client")
    public static class CacheClientProperties {
        /**
         * Enable or disable the cache client.
         */
        private boolean enabled = true;

        /**
         * Connection timeout in milliseconds.
         */
        private int connectTimeout = 5000;

        /**
         * Read timeout in milliseconds.
         */
        private int readTimeout = 10000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
        }
    }

    /**
     * No-op implementation used when cache client is disabled.
     */
    private static class NoOpCacheManagerClient implements CacheManagerClient {
        @Override
        public java.util.Optional<CacheObjectInfo> getObjectInfo(String datasetId, String filepath) {
            return java.util.Optional.empty();
        }

        @Override
        public boolean isCached(String datasetId, String filepath) {
            return false;
        }

        @Override
        public CacheObjectSummary getDatasetSummary(String datasetId) {
            return CacheObjectSummary.builder()
                    .id(datasetId)
                    .totalObjects(0)
                    .cachedObjects(0)
                    .totalSize(0L)
                    .build();
        }

        @Override
        public void queueForCaching(String datasetId, String filepath, boolean recache) {
            // No-op
        }

        @Override
        public void removeFromCache(String datasetId, String filepath) {
            // No-op
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public java.util.Map<String, Object> getResourceMetadata(String datasetId) {
            return java.util.Collections.emptyMap();
        }

        @Override
        public java.util.Map<String, Object> getComponentMetadata(String datasetId, String filepath) {
            return java.util.Collections.emptyMap();
        }

        @Override
        public java.util.Map<String, Object> cacheDatasetForRPA(String datasetId, String version) {
            return java.util.Collections.emptyMap();
        }

        @Override
        public java.util.Map<String, Object> getRPACachedObjects(String randomId) {
            return java.util.Collections.emptyMap();
        }

        @Override
        public boolean uncacheRPA(String randomId) {
            return false;
        }
    }
}
