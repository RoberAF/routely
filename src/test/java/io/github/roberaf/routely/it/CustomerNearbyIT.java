package io.github.roberaf.routely.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import io.github.roberaf.routely.api.dto.CustomerResponse;

/**
 * GET /api/v1/customers/nearby against the real Lugo fixture: ids 1-4 sit
 * inside the city, id 5 is the one inactive row, and everything from 6
 * onward is at least 15 km away.
 */
@Tag("integration")
class CustomerNearbyIT extends PostgisTestSupport {

	private static final String NEARBY_URL = "/api/v1/customers/nearby?lat=43.0121&lng=-7.5559&radiusMeters={radius}";

	@Test
	void twoKilometerRadiusReturnsTheThreeClosestCafeAndShops() {
		String token = loginToken("manager@routely.dev", "manager123");

		ResponseEntity<CustomerResponse[]> response = restTemplate.exchange(NEARBY_URL, HttpMethod.GET,
				authHeaders(token), CustomerResponse[].class, 2000);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		List<CustomerResponse> customers = toList(response);
		assertThat(customers).extracting(CustomerResponse::id).containsExactlyInAnyOrder(1L, 2L, 3L);
		assertThat(customers.get(0).id()).isEqualTo(1L);
	}

	@Test
	void fiveKilometerRadiusAddsThePanaderiaButNeverTheInactiveOrDistantRows() {
		String token = loginToken("manager@routely.dev", "manager123");

		ResponseEntity<CustomerResponse[]> response = restTemplate.exchange(NEARBY_URL, HttpMethod.GET,
				authHeaders(token), CustomerResponse[].class, 5000);

		List<CustomerResponse> customers = toList(response);
		assertThat(customers).extracting(CustomerResponse::id).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
		assertThat(customers).extracting(CustomerResponse::id).doesNotContain(5L, 6L);
	}

	@Test
	void withoutATokenIsUnauthorized() {
		ResponseEntity<String> response = restTemplate.exchange(NEARBY_URL, HttpMethod.GET, HttpEntity.EMPTY,
				String.class, 2000);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(response.getHeaders().getContentType()).hasToString("application/problem+json");
	}

	@Test
	void negativeRadiusIsBadRequest() {
		String token = loginToken("manager@routely.dev", "manager123");

		ResponseEntity<String> response = restTemplate.exchange(NEARBY_URL, HttpMethod.GET, authHeaders(token),
				String.class, -1);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	private static List<CustomerResponse> toList(ResponseEntity<CustomerResponse[]> response) {
		CustomerResponse[] body = response.getBody();
		assertThat(body).isNotNull();
		return Arrays.asList(body);
	}
}
