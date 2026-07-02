package io.github.roberaf.routely.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import io.github.roberaf.routely.security.TokenResponse;

/**
 * Base class for the "real database" integration tests: one PostGIS
 * container for the whole IT suite (started once, in a static initializer,
 * rather than per class) so Flyway only has to run once and the container
 * doesn't get torn down and rebuilt between test classes.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
abstract class PostgisTestSupport {

	protected static final PostgreSQLContainer POSTGIS = new PostgreSQLContainer(
			DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

	static {
		POSTGIS.start();
	}

	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGIS::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGIS::getUsername);
		registry.add("spring.datasource.password", POSTGIS::getPassword);
	}

	@Autowired
	protected TestRestTemplate restTemplate;

	protected String loginToken(String email, String password) {
		ResponseEntity<TokenResponse> response = restTemplate.postForEntity("/api/v1/auth/login",
				Map.of("email", email, "password", password), TokenResponse.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		TokenResponse body = response.getBody();
		assertThat(body).isNotNull();
		return body.accessToken();
	}

	protected HttpEntity<Void> authHeaders(String token) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(token);
		return new HttpEntity<>(headers);
	}

	protected <T> HttpEntity<T> authHeaders(String token, T body) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(token);
		headers.setContentType(MediaType.APPLICATION_JSON);
		return new HttpEntity<>(body, headers);
	}
}
