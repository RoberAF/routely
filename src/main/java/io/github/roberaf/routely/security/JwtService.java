package io.github.roberaf.routely.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import io.github.roberaf.routely.domain.AppUser;
import io.github.roberaf.routely.domain.SalesRep;

/**
 * Issues the access tokens routely hands back from {@code /api/v1/auth/login}.
 * Claim names are a contract the API package reads by name, hence the public
 * constants instead of inline string literals.
 */
@Service
public class JwtService {

	public static final String CLAIM_UID = "uid";
	public static final String CLAIM_ROLE = "role";
	public static final String CLAIM_REP_ID = "repId";

	private static final String ISSUER = "routely";
	private static final long EXPIRES_IN_SECONDS = 8 * 60 * 60;

	private final JwtEncoder jwtEncoder;

	public JwtService(JwtEncoder jwtEncoder) {
		this.jwtEncoder = jwtEncoder;
	}

	public String issueToken(AppUser user) {
		Instant now = Instant.now();

		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
				.issuer(ISSUER)
				.subject(user.getEmail())
				.issuedAt(now)
				.expiresAt(now.plus(EXPIRES_IN_SECONDS, ChronoUnit.SECONDS))
				.claim(CLAIM_UID, user.getId())
				.claim(CLAIM_ROLE, user.getRole().name());

		SalesRep salesRep = user.getSalesRep();
		if (salesRep != null) {
			claims.claim(CLAIM_REP_ID, salesRep.getId());
		}

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
	}

	public long expiresInSeconds() {
		return EXPIRES_IN_SECONDS;
	}
}
