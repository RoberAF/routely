package io.github.roberaf.routely.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import io.github.roberaf.routely.api.dto.RoutePlanResponse;
import io.github.roberaf.routely.api.dto.RouteStopResponse;

/**
 * End-to-end walk through the whole "plan a route" story: a manager computes
 * a plan for a rep, bad input is rejected, and the rep-isolation rules hold
 * up on real persisted data. Ordered because later steps read back plans
 * created by the earlier compute calls.
 */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RoutePlanComputeIT extends PostgisTestSupport {

	private static final String ROUTE_PLANS_URL = "/api/v1/route-plans";

	private long lauraFullFleetPlanId;

	@Test
	@Order(1)
	void managerComputesFullActiveFleetRouteForLaura() {
		String managerToken = loginToken("manager@routely.dev", "manager123");
		Map<String, Object> request = Map.of("repId", 1, "planDate", "2026-07-20");

		ResponseEntity<RoutePlanResponse> response = restTemplate.exchange(ROUTE_PLANS_URL + "/compute",
				HttpMethod.POST, authHeaders(managerToken, request), RoutePlanResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		RoutePlanResponse plan = response.getBody();
		assertThat(plan).isNotNull();
		assertThat(plan.stops()).hasSize(39);

		List<Integer> sequenceIndexes = plan.stops().stream().map(RouteStopResponse::sequenceIndex).sorted().toList();
		assertThat(sequenceIndexes).isEqualTo(IntStream.range(0, 39).boxed().toList());

		assertThat(plan.totalDistanceMeters()).isGreaterThan(0);
		assertThat(plan.totalDistanceMeters()).isLessThanOrEqualTo(plan.naiveDistanceMeters());
		assertThat(plan.improvementPercent()).isGreaterThanOrEqualTo(0);
		assertThat(plan.computedAt()).isNotNull();

		lauraFullFleetPlanId = plan.id();
	}

	@Test
	@Order(2)
	void managerComputesFourStopRouteWithExplicitCustomerIds() {
		String managerToken = loginToken("manager@routely.dev", "manager123");
		Map<String, Object> request = Map.of("repId", 1, "planDate", "2026-07-21", "customerIds", List.of(1, 2, 3, 4));

		ResponseEntity<RoutePlanResponse> response = restTemplate.exchange(ROUTE_PLANS_URL + "/compute",
				HttpMethod.POST, authHeaders(managerToken, request), RoutePlanResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		RoutePlanResponse plan = response.getBody();
		assertThat(plan).isNotNull();
		assertThat(plan.stops()).hasSize(4);
		assertThat(plan.stops()).allSatisfy(stop -> assertThat(stop.estimatedArrival()).isNotNull());
	}

	@Test
	@Order(3)
	void computeWithAnInactiveCustomerIdIsBadRequest() {
		String managerToken = loginToken("manager@routely.dev", "manager123");
		Map<String, Object> request = Map.of("repId", 1, "planDate", "2026-07-22", "customerIds", List.of(1, 2, 5));

		ResponseEntity<String> response = restTemplate.exchange(ROUTE_PLANS_URL + "/compute", HttpMethod.POST,
				authHeaders(managerToken, request), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getHeaders().getContentType()).hasToString("application/problem+json");
	}

	@Test
	@Order(4)
	void computeWithAnUnknownCustomerIdIsNotFound() {
		String managerToken = loginToken("manager@routely.dev", "manager123");
		Map<String, Object> request = Map.of("repId", 1, "planDate", "2026-07-22", "customerIds", List.of(1, 2, 9999));

		ResponseEntity<String> response = restTemplate.exchange(ROUTE_PLANS_URL + "/compute", HttpMethod.POST,
				authHeaders(managerToken, request), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getHeaders().getContentType()).hasToString("application/problem+json");
	}

	@Test
	@Order(5)
	void aRepTokenCannotComputeRoutes() {
		String lauraToken = loginToken("laura@routely.dev", "laura123");
		Map<String, Object> request = Map.of("repId", 1, "planDate", "2026-07-23");

		ResponseEntity<String> response = restTemplate.exchange(ROUTE_PLANS_URL + "/compute", HttpMethod.POST,
				authHeaders(lauraToken, request), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	@Order(6)
	void lauraReadsHerOwnPlansAndIsBlockedFromBraisRepId() {
		String lauraToken = loginToken("laura@routely.dev", "laura123");

		ResponseEntity<RoutePlanResponse[]> ownPlans = restTemplate.exchange(ROUTE_PLANS_URL + "?repId=1",
				HttpMethod.GET, authHeaders(lauraToken), RoutePlanResponse[].class);
		assertThat(ownPlans.getStatusCode()).isEqualTo(HttpStatus.OK);
		RoutePlanResponse[] ownPlansBody = ownPlans.getBody();
		assertThat(ownPlansBody).isNotNull();
		assertThat(Arrays.stream(ownPlansBody).map(RoutePlanResponse::id)).contains(lauraFullFleetPlanId);

		ResponseEntity<String> braisAsLaura = restTemplate.exchange(ROUTE_PLANS_URL + "?repId=2", HttpMethod.GET,
				authHeaders(lauraToken), String.class);
		assertThat(braisAsLaura.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(braisAsLaura.getHeaders().getContentType()).hasToString("application/problem+json");

		ResponseEntity<RoutePlanResponse[]> ownPlansNoParam = restTemplate.exchange(ROUTE_PLANS_URL, HttpMethod.GET,
				authHeaders(lauraToken), RoutePlanResponse[].class);
		assertThat(ownPlansNoParam.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(ownPlansNoParam.getBody()).isNotEmpty();
	}

	@Test
	@Order(7)
	void braisHasNoPlansOfHisOwnYet() {
		String braisToken = loginToken("brais@routely.dev", "brais123");

		ResponseEntity<RoutePlanResponse[]> plans = restTemplate.exchange(ROUTE_PLANS_URL, HttpMethod.GET,
				authHeaders(braisToken), RoutePlanResponse[].class);

		assertThat(plans.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(plans.getBody()).isEmpty();
	}

	@Test
	@Order(8)
	void lauraCanReadHerPlanByIdButBraisCannot() {
		String lauraToken = loginToken("laura@routely.dev", "laura123");
		String braisToken = loginToken("brais@routely.dev", "brais123");
		String planUrl = ROUTE_PLANS_URL + "/" + lauraFullFleetPlanId;

		ResponseEntity<RoutePlanResponse> asLaura = restTemplate.exchange(planUrl, HttpMethod.GET,
				authHeaders(lauraToken), RoutePlanResponse.class);
		assertThat(asLaura.getStatusCode()).isEqualTo(HttpStatus.OK);
		RoutePlanResponse plan = asLaura.getBody();
		assertThat(plan).isNotNull();
		assertThat(plan.id()).isEqualTo(lauraFullFleetPlanId);

		ResponseEntity<String> asBrais = restTemplate.exchange(planUrl, HttpMethod.GET, authHeaders(braisToken),
				String.class);
		assertThat(asBrais.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}
}
