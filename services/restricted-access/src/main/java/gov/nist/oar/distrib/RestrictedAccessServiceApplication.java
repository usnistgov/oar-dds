package gov.nist.oar.distrib;

import gov.nist.oar.common.cache.client.CacheManagerClient;
import gov.nist.oar.common.cache.impl.spring.SpringCacheManagerClient;
import gov.nist.oar.common.cache.impl.spring.feign.CacheManagementFeignClient;
import gov.nist.oar.distrib.service.rpa.HttpURLConnectionRPARequestHandlerService;
import gov.nist.oar.distrib.service.rpa.RPADatasetCacher;
import gov.nist.oar.distrib.service.rpa.RPARequestHandler;
import gov.nist.oar.distrib.service.rpa.RemoteRPADatasetCacher;
import gov.nist.oar.distrib.web.RPAConfiguration;
import gov.nist.oar.distrib.web.RPAAsyncExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

/**
 * Main application class for the Restricted Access Service.
 * <p>
 * This service handles Restricted Public Access (RPA) requests, managing the workflow
 * for requesting access to restricted datasets. It communicates with:
 * <ul>
 *   <li>Salesforce - for record management (create, read, update RPA requests)</li>
 *   <li>cache-mgmt-service - for caching datasets (via Feign client)</li>
 *   <li>Metadata resolver - for checking pre-approval status</li>
 * </ul>
 * <p>
 * <b>Architecture Note:</b> This service delegates ALL caching operations to the
 * cache-mgmt-service via HTTP (Feign). It does not perform local caching.
 * This follows the microservices pattern where cache-mgmt owns all cache infrastructure.
 * <p>
 * The original monolith had two RPADatasetCacher implementations:
 * <ul>
 *   <li>DefaultRPADatasetCacher - uses local RPACachingService (monolith pattern)</li>
 *   <li>HttpRPADatasetCacher - calls remote caching endpoint (planned but unused in monolith)</li>
 * </ul>
 * In this microservices version, we use RemoteRPADatasetCacher which delegates to
 * cache-mgmt-service, similar in spirit to the original HttpRPADatasetCacher.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackageClasses = CacheManagementFeignClient.class)
@EnableConfigurationProperties(RPAConfiguration.class)
public class RestrictedAccessServiceApplication {

    private static final Logger logger = LoggerFactory.getLogger(RestrictedAccessServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(RestrictedAccessServiceApplication.class, args);
    }

    /**
     * Creates the CacheManagerClient for communicating with cache-mgmt-service.
     * <p>
     * This client wraps the Feign client and provides a clean interface for
     * cache operations like caching datasets for RPA and uncaching.
     *
     * @param feignClient the Feign client auto-wired by Spring Cloud
     * @return the cache manager client
     */
    @Bean
    public CacheManagerClient cacheManagerClient(CacheManagementFeignClient feignClient) {
        logger.info("Creating CacheManagerClient for communication with cache-mgmt-service");
        return new SpringCacheManagerClient(feignClient);
    }

    /**
     * Creates the RPADatasetCacher that delegates to cache-mgmt-service.
     * <p>
     * This implementation replaces the monolith's DefaultRPADatasetCacher which
     * used local RPACachingService. In the microservices architecture, all caching
     * is centralized in cache-mgmt-service.
     * <p>
     * The RemoteRPADatasetCacher calls these cache-mgmt endpoints:
     * <ul>
     *   <li>PUT /cache/rpa/{datasetId} - cache a dataset for RPA access</li>
     *   <li>DELETE /cache/rpa/objects/{randomId} - uncache RPA session</li>
     * </ul>
     *
     * @param cacheManagerClient the client for HTTP communication with cache-mgmt
     * @return the remote RPA dataset cacher
     */
    @Bean
    public RPADatasetCacher rpaDatasetCacher(CacheManagerClient cacheManagerClient) {
        logger.info("Creating RemoteRPADatasetCacher - all RPA caching delegated to cache-mgmt-service");
        return new RemoteRPADatasetCacher(cacheManagerClient);
    }

    /**
     * Creates the RPA request handler for managing Salesforce records.
     * <p>
     * This handler orchestrates the RPA workflow:
     * <ol>
     *   <li>Validates user against blacklist</li>
     *   <li>Checks dataset pre-approval status via resolver</li>
     *   <li>Creates/updates records in Salesforce</li>
     *   <li>Triggers caching via RPADatasetCacher when approved</li>
     *   <li>Sends notification emails</li>
     * </ol>
     * <p>
     * The constructor signature matches the monolith's pattern:
     * {@code HttpURLConnectionRPARequestHandlerService(config, cachingService, datasetCacher)}
     * where cachingService is null (not used) and datasetCacher is the remote implementation.
     *
     * @param rpaConfiguration the RPA configuration
     * @param rpaDatasetCacher the dataset cacher (RemoteRPADatasetCacher)
     * @return the RPA request handler
     */
    @Bean
    public RPARequestHandler rpaRequestHandler(RPAConfiguration rpaConfiguration,
                                                RPADatasetCacher rpaDatasetCacher) {
        logger.info("Creating RPARequestHandler with RemoteRPADatasetCacher");
        // Pass null for RPACachingService since we use the RPADatasetCacher directly
        return new HttpURLConnectionRPARequestHandlerService(
                rpaConfiguration,
                null,  // No local RPACachingService - we use remote caching
                rpaDatasetCacher
        );
    }

    /**
     * Creates the async executor for handling long-running RPA operations.
     * <p>
     * This executor wraps the RPARequestHandler and provides async execution
     * for operations like caching that may take time.
     *
     * @param handler the RPA request handler
     * @return the async executor
     */
    @Bean
    public RPAAsyncExecutor rpaAsyncExecutor(RPARequestHandler handler) {
        logger.info("Creating RPAAsyncExecutor");
        return new RPAAsyncExecutor(handler);
    }
}
