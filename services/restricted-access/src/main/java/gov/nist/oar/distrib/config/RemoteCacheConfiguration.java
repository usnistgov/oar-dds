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
package gov.nist.oar.distrib.config;

import gov.nist.oar.common.cache.client.CacheManagerClient;
import gov.nist.oar.common.cache.impl.spring.SpringCacheManagerClient;
import gov.nist.oar.common.cache.impl.spring.feign.CacheManagementFeignClient;
import gov.nist.oar.distrib.service.rpa.RPADatasetCacher;
import gov.nist.oar.distrib.service.rpa.RemoteRPADatasetCacher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for remote cache mode in the restricted-access service.
 * <p>
 * When <code>distrib.cache.mode=remote</code>, this configuration activates and provides:
 * <ul>
 *   <li>A {@link CacheManagerClient} for communicating with the cache-mgmt service</li>
 *   <li>A {@link RemoteRPADatasetCacher} that delegates RPA caching to cache-mgmt</li>
 * </ul>
 * <p>
 * This is the correct microservices architecture where all cache operations go through
 * the centralized cache-mgmt service. In the monolith, RPA caching used the same
 * PDRCacheManager/BasicCache infrastructure as regular caching, with role-based volume
 * selection (ROLE_RESTRICTED_DATA). This remote implementation maintains that behavior
 * by delegating to cache-mgmt which has the same infrastructure.
 *
 * @see gov.nist.oar.distrib.service.rpa.RemoteRPADatasetCacher
 */
@Configuration
@ConditionalOnProperty(name = "distrib.cache.mode", havingValue = "remote")
@EnableFeignClients(basePackageClasses = CacheManagementFeignClient.class)
public class RemoteCacheConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RemoteCacheConfiguration.class);

    /**
     * Creates the CacheManagerClient bean for communicating with cache-mgmt service.
     *
     * @param feignClient the Feign client auto-wired by Spring Cloud
     * @return the cache manager client
     */
    @Bean
    public CacheManagerClient cacheManagerClient(CacheManagementFeignClient feignClient) {
        logger.info("Creating CacheManagerClient for remote cache mode");
        return new SpringCacheManagerClient(feignClient);
    }

    /**
     * Creates the RPADatasetCacher bean that uses remote cache via cache-mgmt service.
     * <p>
     * This is marked as @Primary so it takes precedence over any local cache implementation.
     *
     * @param cacheManagerClient the client for communicating with cache-mgmt
     * @return the remote RPA dataset cacher
     */
    @Bean
    @Primary
    public RPADatasetCacher rpaDatasetCacher(CacheManagerClient cacheManagerClient) {
        logger.info("Creating RemoteRPADatasetCacher for RPA operations via cache-mgmt service");
        return new RemoteRPADatasetCacher(cacheManagerClient);
    }
}
