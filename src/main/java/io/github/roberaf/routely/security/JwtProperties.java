package io.github.roberaf.routely.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Symmetric secret used to sign and verify the access tokens this service
 * issues. Boot validates presence via {@link org.springframework.boot.context.properties.bind.ConstructorBinding}
 * wiring; the length check below guards against a secret too short for
 * HS256 (which needs at least 256 bits, i.e. 32 ASCII bytes).
 */
@ConfigurationProperties(prefix = "routely.security")
public record JwtProperties(String jwtSecret) {

	public JwtProperties {
		if (jwtSecret == null || jwtSecret.length() < 32) {
			throw new IllegalStateException(
					"routely.security.jwt-secret must be set and at least 32 characters long");
		}
	}
}
