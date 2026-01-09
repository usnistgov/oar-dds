package gov.nist.oar.common.datasetaccess.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing file information from the dataset-access service.
 * <p>
 * This matches the FileDescription class used in dataset-access,
 * containing metadata about a downloadable file.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileInfo {

    /**
     * The file name
     */
    @JsonProperty("name")
    private String name;

    /**
     * The content length in bytes
     */
    @JsonProperty("contentLength")
    private long contentLength;

    /**
     * The content type (MIME type)
     */
    @JsonProperty("contentType")
    private String contentType;

    /**
     * The checksum of the file (if available)
     */
    @JsonProperty("checksum")
    private ChecksumInfo checksum;

    /**
     * The AIP ID this file belongs to
     */
    @JsonProperty("aipid")
    private String aipId;

    /**
     * The download URL for this file
     */
    @JsonProperty("downloadURL")
    private String downloadUrl;

    /**
     * The version this file was introduced
     */
    @JsonProperty("sinceVersion")
    private String sinceVersion;

    /**
     * Nested DTO for checksum information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChecksumInfo {
        @JsonProperty("hash")
        private String hash;

        @JsonProperty("algorithm")
        private String algorithm;
    }
}
