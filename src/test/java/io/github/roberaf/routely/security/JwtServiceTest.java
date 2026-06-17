package io.github.roberaf.routely.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import io.github.roberaf.routely.domain.AppUser;
import io.github.roberaf.routely.domain.Role;
import io.github.roberaf.routely.domain.SalesRep;

/**
 * Encodes and decodes tokens with hand-built encoder/decoder instances
 * (same construction {@link SecurityConfig} uses) so this stays a plain
 * unit test with no Spring context to boot.
 */
class JwtServiceTest {

	private static final String TEST_SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

	private final JwtEncoder jwtEncoder = buildEncoder();
	private final JwtDecoder jwtDecoder = buildDecoder();
	private final JwtService jwtService = new JwtService(jwtEncoder);

	private static JwtEncoder buildEncoder() {
		SecretKeySpec key = new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		return new NimbusJwtEncoder(new ImmutableSecret<>(key));
	}

	private static JwtDecoder buildDecoder() {
		SecretKeySpec key = new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
	}

	private static AppUser userWithId(long id, String email, Role role, SalesRep salesRep) {
		AppUser user = new AppUser(email, "irrelevant-hash", role, salesRep);
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private static SalesRep salesRepWithId(long id) {
		SalesRep salesRep = new SalesRep("Laura", "laura@routely.dev", null);
		ReflectionTestUtils.setField(salesRep, "id", id);
		return salesRep;
	}

	@Test
	void issuesTokenWithRepClaimsForARep() {
		SalesRep salesRep = salesRepWithId(7L);
		AppUser rep = userWithId(3L, "laura@routely.dev", Role.REP, salesRep);

		Jwt decoded = jwtDecoder.decode(jwtService.issueToken(rep));

		assertThat(decoded.getSubject()).isEqualTo("laura@routely.dev");
		assertThat(decoded.getClaimAsString(JwtService.CLAIM_ROLE)).isEqualTo("REP");
		assertThat(decoded.<Long>getClaim(JwtService.CLAIM_UID)).isEqualTo(3L);
		assertThat(decoded.<Long>getClaim(JwtService.CLAIM_REP_ID)).isEqualTo(7L);
		assertThat(decoded.getExpiresAt()).isCloseTo(Instant.now().plus(8, ChronoUnit.HOURS), within(5,
				ChronoUnit.SECONDS));
	}

	@Test
	void issuesTokenWithoutRepClaimForAnAdmin() {
		AppUser admin = userWithId(1L, "admin@routely.dev", Role.ADMIN, null);

		Jwt decoded = jwtDecoder.decode(jwtService.issueToken(admin));

		assertThat(decoded.getSubject()).isEqualTo("admin@routely.dev");
		assertThat(decoded.getClaimAsString(JwtService.CLAIM_ROLE)).isEqualTo("ADMIN");
		assertThat(decoded.<Long>getClaim(JwtService.CLAIM_UID)).isEqualTo(1L);
		assertThat(decoded.hasClaim(JwtService.CLAIM_REP_ID)).isFalse();
	}
}
