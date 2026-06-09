package io.github.roberaf.routely.optimizer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class NearestNeighborTest {

	private static final GeoPoint HOME = new GeoPoint(0, 0);

	@Test
	void picksTheNearestUnvisitedStopEachTime() {
		Visit far = new Visit(30, new GeoPoint(0, 0.03), null, null);
		Visit near = new Visit(10, new GeoPoint(0, 0.01), null, null);
		Visit middle = new Visit(20, new GeoPoint(0, 0.02), null, null);

		List<Visit> order = NearestNeighbor.buildOrder(HOME, List.of(far, near, middle));

		assertThat(order).extracting(Visit::customerId).containsExactly(10L, 20L, 30L);
	}

	@Test
	void tieBreaksOnExactDistanceTiesByLowerCustomerId() {
		Visit higherId = new Visit(7, new GeoPoint(0, 0.02), null, null);
		Visit lowerId = new Visit(3, new GeoPoint(0, -0.02), null, null);

		// Both stops are exactly Haversine.distanceMeters(HOME, ...) apart from home.
		assertThat(Haversine.distanceMeters(HOME, higherId.location()))
				.isEqualTo(Haversine.distanceMeters(HOME, lowerId.location()));

		List<Visit> order = NearestNeighbor.buildOrder(HOME, List.of(higherId, lowerId));

		assertThat(order).extracting(Visit::customerId).containsExactly(3L, 7L);
	}
}
