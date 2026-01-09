package gov.nist.oar.common.cache.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Information about a cached object.
 * <p>
 * This DTO matches the JSON response from GET /cache/objects/{dsid}/{filepath}
 * as returned by CacheManagementController.toJSONObject()
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CacheObjectInfo {

    /**
     * Unique identifier of the cached object (dsid/filepath).
     */
    private String id;

    /**
     * The file name.
     */
    private String name;

    /**
     * Whether the object is currently cached.
     */
    private Boolean cached;

    /**
     * The cache volume name where the object is stored (e.g., "fst0", "gen0").
     */
    private String volume;

    /**
     * Size of the object in bytes.
     */
    private Long size;

    /**
     * Cache score/priority (higher = more important to keep cached).
     */
    private Integer score;

    /**
     * Checksum of the object for integrity verification.
     */
    private String checksum;

    /**
     * MIME type of the cached object.
     */
    private String mimetype;

    /**
     * Timestamp when the object was added to cache (milliseconds since epoch).
     */
    private Long since;

    /**
     * Number of times this object has been retrieved.
     */
    private Integer ngets;

    /**
     * Priority of this cached object.
     */
    private Integer priority;

    /**
     * Additional metadata fields from the cache object.
     */
    @JsonProperty("metadata")
    private Map<String, Object> additionalMetadata;

    /**
     * Get the full volume path to the cached file.
     * This is a helper method since the actual file path depends on the volume configuration.
     *
     * @return the volume name if cached, null otherwise
     */
    public String getVolumeName() {
        return volume;
    }

    /**
     * Check if the object is currently available in the cache.
     *
     * @return true if cached, false otherwise
     */
    public boolean isAvailable() {
        return Boolean.TRUE.equals(cached);
    }
}
