package io.github.roberaf.routely.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Pins the demo credentials documented for the seed data: whoever changes
 * V2__seed_data.sql must keep these four logins working, or update this test
 * (and the documentation) deliberately.
 */
class SeedPasswordsTest {

	private static final Path SEED_FILE = Path.of("src/main/resources/db/migration/V2__seed_data.sql");
	private static final Pattern BCRYPT_HASH = Pattern.compile("\\$2[aby]\\$[0-9]{2}\\$[./A-Za-z0-9]{53}");

	private static final Map<String, String> DOCUMENTED_PASSWORDS_BY_EMAIL = Map.of(
			"admin@routely.dev", "admin123",
			"manager@routely.dev", "manager123",
			"laura@routely.dev", "laura123",
			"brais@routely.dev", "brais123");

	@Test
	void seedPasswordHashesMatchDocumentedDemoPasswords() throws IOException {
		String seedSql = Files.readString(SEED_FILE);
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

		for (Map.Entry<String, String> entry : DOCUMENTED_PASSWORDS_BY_EMAIL.entrySet()) {
			String email = entry.getKey();
			String password = entry.getValue();
			String hash = hashForEmail(seedSql, email);

			assertThat(encoder.matches(password, hash))
					.as("password '%s' should match the seeded bcrypt hash for %s", password, email)
					.isTrue();
		}
	}

	private static String hashForEmail(String seedSql, String email) {
		return seedSql.lines()
				.filter(line -> line.contains(email))
				.findFirst()
				.map(line -> {
					Matcher matcher = BCRYPT_HASH.matcher(line);
					if (!matcher.find()) {
						throw new AssertionError("no bcrypt hash found on the seed line for " + email);
					}
					return matcher.group();
				})
				.orElseThrow(() -> new AssertionError("no seed line found for " + email));
	}
}
