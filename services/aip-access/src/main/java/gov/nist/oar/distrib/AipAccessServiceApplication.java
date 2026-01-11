package gov.nist.oar.distrib;

import java.io.FileNotFoundException;
import java.io.InputStream;

import jakarta.activation.MimetypesFileTypeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

import gov.nist.oar.distrib.BagStorage;
import gov.nist.oar.distrib.StorageVolumeException;
import gov.nist.oar.distrib.service.DefaultPreservationBagService;
import gov.nist.oar.distrib.service.PreservationBagService;
import gov.nist.oar.distrib.storage.AWSS3LongTermStorage;
import gov.nist.oar.distrib.storage.FilesystemLongTermStorage;
import gov.nist.oar.distrib.web.ConfigurationException;

import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * AIP Access Service - provides access to preservation AIPs (bags).
 * Extracts functionality from AIPAccessController.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableAsync
public class AipAccessServiceApplication {

    private static Logger logger = LoggerFactory.getLogger(AipAccessServiceApplication.class);

    @Value("${distrib.bagstore.location}")
    String bagstore;

    @Value("${distrib.bagstore.mode}")
    String mode;

    @Value("${cloud.aws.region:us-east-1}")
    String region;

    /**
     * The storage service to use to access the bags
     */
    @Bean
    public BagStorage getLongTermStorage(S3Client s3client) throws ConfigurationException {
        logger.info("Bagstore mode: " + mode);
        logger.info("Bagstore location: " + bagstore);
        try {
            if (mode.equals("aws") || mode.equals("remote")) {
                return new AWSS3LongTermStorage(bagstore, s3client);
            } else if (mode.equals("local")) {
                return new FilesystemLongTermStorage(bagstore);
            } else {
                throw new ConfigurationException("distrib.bagstore.mode",
                        "Unsupported storage mode: " + mode);
            }
        } catch (FileNotFoundException ex) {
            throw new ConfigurationException("distrib.bagstore.location",
                    "Storage Location not found: " + ex.getMessage(), ex);
        } catch (StorageVolumeException ex) {
            throw new ConfigurationException("distrib.bagstore.aws",
                    "Storage volume exception: " + ex.getMessage(), ex);
        }
    }

    /**
     * The client for accessing S3 storage
     */
    @Bean
    public S3Client getAmazonS3() throws ConfigurationException {
        logger.info("Creating S3 client");

        if ("remote".equalsIgnoreCase(mode)) {
            throw new ConfigurationException("Remote credentials not supported yet");
        }

        try {
            S3Client client = S3Client.builder()
                    .credentialsProvider(InstanceProfileCredentialsProvider.create())
                    .region(Region.of(region))
                    .build();

            logger.info("S3 client created successfully for region: {}", region);
            return client;
        } catch (Exception e) {
            logger.error("Failed to create S3 client: {}", e.getMessage(), e);
            throw new ConfigurationException("Error creating S3 client: " + e.getMessage(), e);
        }
    }

    /**
     * The MIME type assignments to use when setting content types
     */
    @Bean
    public MimetypesFileTypeMap getMimetypesFileTypeMap() {
        InputStream mis = getClass().getResourceAsStream("/mime.types");
        if (mis == null) {
            logger.warn("No mime.type resource found; content type support will be limited!");
            return new MimetypesFileTypeMap();
        }
        return new MimetypesFileTypeMap(mis);
    }

    /**
     * The service implementation to use to access preservation bags
     */
    @Bean
    public PreservationBagService getPreservationBagService(BagStorage lts, MimetypesFileTypeMap mimemap) {
        return new DefaultPreservationBagService(lts, mimemap);
    }

    /**
     * Configure MVC model, including setting CORS support and semicolon in URLs.
     */
    @Bean
    public WebMvcConfigurer mvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**");
            }

            @Override
            public void configurePathMatch(PathMatchConfigurer configurer) {
                UrlPathHelper urlPathHelper = configurer.getUrlPathHelper();
                if (urlPathHelper == null) {
                    urlPathHelper = new UrlPathHelper();
                    configurer.setUrlPathHelper(urlPathHelper);
                }
                urlPathHelper.setRemoveSemicolonContent(false);
            }
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(AipAccessServiceApplication.class, args);
    }
}
