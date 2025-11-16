package gov.nist.oar.distrib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

import gov.nist.oar.distrib.service.DataPackagingService;
import gov.nist.oar.distrib.service.DefaultDataPackagingService;

/**
 * Data Bundle Access Service - provides access to pre-packaged data bundles.
 * Extracts functionality from DataBundleAccessController.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class DataBundleServiceApplication {

    private static Logger logger = LoggerFactory.getLogger(DataBundleServiceApplication.class);

    @Value("${distrib.packaging.maxpackagesize:500000000}")
    long maxPkgSize;

    @Value("${distrib.packaging.maxfilecount:200}")
    int maxFileCount;

    @Value("${distrib.packaging.allowedurls:}")
    String allowedUrls;

    @Value("${distrib.packaging.allowedRedirects:1}")
    int allowedRedirects;

    /**
     * The service implementation to use to package data into bundles
     */
    @Bean
    public DataPackagingService getDataPackagingService() {
        return new DefaultDataPackagingService(allowedUrls, maxPkgSize, maxFileCount, allowedRedirects);
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
        SpringApplication.run(DataBundleServiceApplication.class, args);
    }
}
