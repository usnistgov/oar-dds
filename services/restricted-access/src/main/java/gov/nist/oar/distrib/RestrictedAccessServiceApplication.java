package gov.nist.oar.distrib;

import gov.nist.oar.distrib.storage.AWSS3LongTermStorage;
import gov.nist.oar.distrib.storage.FilesystemLongTermStorage;
import gov.nist.oar.distrib.web.ConfigurationException;
import gov.nist.oar.distrib.web.NISTCacheManagerConfig;
import gov.nist.oar.distrib.web.RPAConfiguration;
import gov.nist.oar.distrib.web.RPACachingServiceProvider;
import gov.nist.oar.distrib.web.RPAServiceProvider;
import gov.nist.oar.distrib.web.RPAAsyncExecutor;
import gov.nist.oar.distrib.service.RPACachingService;
import gov.nist.oar.distrib.service.rpa.RPARequestHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.FileNotFoundException;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableConfigurationProperties(RPAConfiguration.class)
public class RestrictedAccessServiceApplication {

    @Value("${distrib.bagstore.location:/usr/local/oar-dist-service/data}")
    String publicBagstoreLocation;

    @Value("${distrib.bagstore.mode:local}")
    String bagstoreMode;

    @Value("${cloud.aws.region:us-east-1}")
    String awsRegion;

    public static void main(String[] args) {
        SpringApplication.run(RestrictedAccessServiceApplication.class, args);
    }

    @Bean
    public S3Client getS3Client() {
        return S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public BagStorage getPublicBagStorage(S3Client s3client) throws ConfigurationException {
        try {
            if (bagstoreMode.equals("aws") || bagstoreMode.equals("remote")) {
                return new AWSS3LongTermStorage(publicBagstoreLocation, s3client);
            } else {
                return new FilesystemLongTermStorage(publicBagstoreLocation);
            }
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("distrib.bagstore.location: Storage Location not found: " + e.getMessage(), e);
        } catch (StorageVolumeException e) {
            throw new ConfigurationException("distrib.bagstore: Storage configuration error: " + e.getMessage(), e);
        }
    }

    @Bean
    public RPACachingServiceProvider getRPACachingServiceProvider(
            NISTCacheManagerConfig cmConfig,
            RPAConfiguration rpaConfig,
            BagStorage publicBagStorage,
            S3Client s3client) {
        return new RPACachingServiceProvider(cmConfig, rpaConfig, publicBagStorage, s3client);
    }

    @Bean
    public RPAServiceProvider getRPAServiceProvider(RPAConfiguration rpaConfig) {
        return new RPAServiceProvider(rpaConfig);
    }

    @Bean
    public RPACachingService getRPACachingService(RPACachingServiceProvider provider, S3Client s3client) throws Exception {
        return provider.getRPACachingService(s3client);
    }

    @Bean
    public RPARequestHandler getRPARequestHandler(RPAServiceProvider provider, RPACachingService cachingService) {
        return provider.getRPARequestHandler(cachingService);
    }

    @Bean
    public RPAAsyncExecutor getRPAAsyncExecutor(RPARequestHandler handler) {
        return new RPAAsyncExecutor(handler);
    }
}
