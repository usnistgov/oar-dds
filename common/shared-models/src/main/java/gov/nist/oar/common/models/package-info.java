/**
 * Common data transfer objects (DTOs) and domain models shared across all microservices.
 *
 * <p>This package contains:
 * <ul>
 *   <li>DTOs for inter-service communication</li>
 *   <li>Domain models representing core business entities</li>
 *   <li>Interfaces defining contracts</li>
 * </ul>
 *
 * <p><b>Design Guidelines:</b>
 * <ul>
 *   <li>Models should be immutable where possible (use Lombok @Value)</li>
 *   <li>No business logic in models - pure data containers only</li>
 *   <li>Use Jackson annotations for JSON serialization</li>
 * </ul>
 */
package gov.nist.oar.common.models;
