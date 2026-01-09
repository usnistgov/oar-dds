package gov.nist.oar.common.cache.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Summary information about a dataset's cached objects.
 * <p>
 * This DTO matches the JSON response from GET /cache/objects/{dsid}
 * as returned by CacheManagementController.summarizeDataset()
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CacheObjectSummary {

    /**
     * The dataset identifier.
     */
    private String id;

    /**
     * Total number of objects from this dataset known to the cache.
     */
    private Integer totalObjects;

    /**
     * Number of objects currently cached.
     */
    private Integer cachedObjects;

    /**
     * Total size of cached objects in bytes.
     */
    private Long totalSize;

    /**
     * List of files in the dataset with their cache information.
     */
    private List<CacheObjectInfo> files;

    /**
     * Additional summary metadata.
     */
    private Map<String, Object> metadata;
}
