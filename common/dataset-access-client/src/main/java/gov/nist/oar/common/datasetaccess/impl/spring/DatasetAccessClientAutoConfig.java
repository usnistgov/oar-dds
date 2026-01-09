package gov.nist.oar.common.datasetaccess.impl.spring;

import gov.nist.oar.common.datasetaccess.client.DatasetAccessClient;
import gov.nist.oar.common.datasetaccess.impl.spring.feign.DatasetAccessFeignClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot Auto-Configuration for the Dataset Access Client.
 * <p>
 * This configuration is automatically activated when:
 * <ul>
 *   <li>Spring Cloud OpenFeign is on the classpath</li>
 *   <li>No other DatasetAccessClient bean is defined</li>
 * </ul>
 * <p>
 * To use this client in your service:
 * <ol>
 *   <li>Add dataset-access-client dependency to your pom.xml</li>
 *   <li>Enable Feign clients in your application or rely on auto-configuration</li>
 *   <li>Inject DatasetAccessClient where needed</li>
 * </ol>
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * @Autowired
 * private DatasetAccessClient datasetAccessClient;
 *
 * public void validateFiles(List<String> files) {
 *     for (String file : files) {
 *         FileValidationResult result = datasetAccessClient.validateFile("mds2-2106", file);
 *         if (result.isAccessible()) {
 *             // File is valid, size is result.getSize()
 *         }
 *     }
 * }
 * }
 * </pre>
 */
@Configuration
@ConditionalOnClass(DatasetAccessFeignClient.class)
@EnableFeignClients(basePackageClasses = DatasetAccessFeignClient.class)
public class DatasetAccessClientAutoConfig {

    /**
     * Create the DatasetAccessClient bean.
     * <p>
     * This bean will only be created if no other DatasetAccessClient is defined.
     *
     * @param feignClient the auto-wired Feign client
     * @return the DatasetAccessClient implementation
     */
    @Bean
    @ConditionalOnMissingBean(DatasetAccessClient.class)
    public DatasetAccessClient datasetAccessClient(DatasetAccessFeignClient feignClient) {
        return new SpringDatasetAccessClient(feignClient);
    }
}
