package gov.nist.oar.distrib;

import gov.nist.oar.distrib.storage.AWSS3LongTermStorage;
import gov.nist.oar.distrib.storage.FilesystemLongTermStorage;
import gov.nist.oar.distrib.web.ConfigurationException;
import gov.nist.oar.distrib.web.NISTCacheManagerConfig;
import gov.nist.oar.distrib.web.CacheManagerProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
public class CacheManagementServiceApplication {

    @Value("${distrib.bagstore.location:/usr/local/oar-dist-service/data}")
    String publicBagstoreLocation;

    @Value("${distrib.bagstore.mode:local}")
    String bagstoreMode;

    @Value("${cloud.aws.region:us-east-1}")
    String awsRegion;

    public static void main(String[] args) {
        SpringApplication.run(CacheManagementServiceApplication.class, args);
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
    public CacheManagerProvider getCacheManagerProvider(NISTCacheManagerConfig config, BagStorage bagStorage, S3Client s3client) {
        return new CacheManagerProvider(config, bagStorage, s3client);
    }
}
