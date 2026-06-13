package io.github.roberaf.routely.optimizer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class TimeWindowTest {

	private static final GeoPoint HOME = new GeoPoint(0, 0);

	private final RouteOptimizer optimizer = new RouteOptimizer();

	@Test
	void nearestNeighborSkipsAnInfeasibleCloserStopForAFeasibleFartherOne() {
		// Travel time home->A is ~160s, so a 09:00-09:01 window is unreachable.
		Visit infeasible = new Visit(1, new GeoPoint(0, 0.02), LocalTime.of(9, 0), LocalTime.of(9, 1));
		Visit feasible = new Visit(2, new GeoPoint(0, 0.05), null, null);

		List<Visit> order = NearestNeighbor.buildOrder(HOME, List.of(infeasible, feasible));

		assertThat(order).extracting(Visit::customerId).containsExactly(2L, 1L);

		OptimizationResult result = optimizer.optimize(HOME, List.of(infeasible, feasible));

		assertThat(result.timeWindowViolations()).isEqualTo(1);
		ScheduledVisit scheduledInfeasible = result.stops().stream()
				.filter(stop -> stop.visit().customerId() == 1)
				.findFirst()
				.orElseThrow();
		assertThat(scheduledInfeasible.withinWindow()).isFalse();
	}

	@Test
	void arrivingBeforeWindowOpensWaitsUntilItOpens() {
		Visit visit = new Visit(1, new GeoPoint(0, 0.02), LocalTime.of(10, 0), LocalTime.of(11, 0));

		List<ScheduledVisit> stops = ScheduleEstimator.schedule(HOME, List.of(visit));

		assertThat(stops).hasSize(1);
		assertThat(stops.get(0).estimatedArrival()).isEqualTo(LocalTime.of(10, 0));
		assertThat(stops.get(0).withinWindow()).isTrue();
		assertThat(ScheduleEstimator.countViolations(stops)).isZero();
	}

	@Test
	void noViolationsWhenNoVisitHasATimeWindow() {
		Visit first = new Visit(1, new GeoPoint(0, 0.02), null, null);
		Visit second = new Visit(2, new GeoPoint(0, 0.05), null, null);

		OptimizationResult result = optimizer.optimize(HOME, List.of(first, second));

		assertThat(result.timeWindowViolations()).isZero();
	}

	@Test
	void scheduledArrivalTimesAreNonDecreasingAlongTheRoute() {
		Visit first = new Visit(1, new GeoPoint(0, 0.02), null, null);
		Visit second = new Visit(2, new GeoPoint(0, 0.05), null, null);
		Visit third = new Visit(3, new GeoPoint(0.01, 0.08), null, null);

		List<ScheduledVisit> stops = ScheduleEstimator.schedule(HOME, List.of(first, second, third));

		LocalTime previous = RouteOptimizer.DEPARTURE_TIME;
		for (ScheduledVisit stop : stops) {
			assertThat(stop.estimatedArrival()).isAfterOrEqualTo(previous);
			previous = stop.estimatedArrival();
		}
	}
}
