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

import gov.nist.oar.distrib.service.rpa.RPADatasetCacher;
import gov.nist.oar.distrib.service.rpa.RPARequestHandler;
import gov.nist.oar.distrib.service.rpa.HttpURLConnectionRPARequestHandlerService;
import gov.nist.oar.distrib.web.RPAConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the RPA service components in remote cache mode.
 * <p>
 * This configuration creates the RPA request handler with the RemoteRPADatasetCacher
 * when <code>distrib.cache.mode=remote</code>.
 * <p>
 * In remote mode, RPA caching operations are delegated to the centralized cache-mgmt
 * service, which maintains the cache inventory in the shared PostgreSQL database.
 * This is the correct microservices architecture.
 */
@Configuration
@ConditionalOnProperty(name = "distrib.cache.mode", havingValue = "remote")
public class RPAServiceConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RPAServiceConfiguration.class);

    @Autowired
    private RPAConfiguration rpaConfiguration;

    @Autowired
    private RPADatasetCacher rpaDatasetCacher;

    /**
     * Creates the RPA request handler bean configured for remote cache mode.
     * <p>
     * The RPADatasetCacher from RemoteCacheConfiguration is injected, which
     * delegates all caching operations to the cache-mgmt service.
     *
     * @return the RPA request handler
     */
    @Bean
    public RPARequestHandler rpaRequestHandler() {
        logger.info("Creating RPA request handler with remote cache mode using: {}",
                rpaDatasetCacher.getClass().getSimpleName());

        // Use the constructor that accepts an RPADatasetCacher
        return new HttpURLConnectionRPARequestHandlerService(
                rpaConfiguration,
                null,  // No local RPACachingService needed in remote mode
                rpaDatasetCacher
        );
    }
}
