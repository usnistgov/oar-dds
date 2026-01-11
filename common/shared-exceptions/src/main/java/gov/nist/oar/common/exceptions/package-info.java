/**
 * Common exception classes shared across all microservices.
 *
 * <p>This package contains:
 * <ul>
 *   <li>Base exception classes</li>
 *   <li>Domain-specific exceptions (ResourceNotFoundException, etc.)</li>
 *   <li>Technical exceptions (ConfigurationException, etc.)</li>
 * </ul>
 *
 * <p><b>Design Guidelines:</b>
 * <ul>
 *   <li>Extend RuntimeException for unchecked exceptions</li>
 *   <li>Include meaningful error messages and context</li>
 *   <li>Support exception chaining (cause)</li>
 * </ul>
 */
package gov.nist.oar.common.exceptions;
