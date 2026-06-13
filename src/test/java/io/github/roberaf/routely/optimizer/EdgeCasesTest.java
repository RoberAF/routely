package io.github.roberaf.routely.optimizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.util.List;

import org.junit.jupiter.api.Test;

class EdgeCasesTest {

	private static final GeoPoint HOME = new GeoPoint(43.0121, -7.5559);

	private final RouteOptimizer optimizer = new RouteOptimizer();

	@Test
	void emptyVisitListYieldsAnEmptyNoOpResult() {
		OptimizationResult result = optimizer.optimize(HOME, List.of());

		assertThat(result.stops()).isEmpty();
		assertThat(result.naiveDistanceMeters()).isZero();
		assertThat(result.nearestNeighborDistanceMeters()).isZero();
		assertThat(result.optimizedDistanceMeters()).isZero();
		assertThat(result.improvementPercent()).isZero();
		assertThat(result.timeWindowViolations()).isZero();
	}

	@Test
	void singleVisitIsJustThereAndBack() {
		Visit onlyStop = new Visit(1, new GeoPoint(43.05, -7.6), null, null);
		double expected = 2 * Haversine.distanceMeters(HOME, onlyStop.location());

		OptimizationResult result = optimizer.optimize(HOME, List.of(onlyStop));

		assertThat(result.naiveDistanceMeters()).isCloseTo(expected, offset(1e-6));
		assertThat(result.nearestNeighborDistanceMeters()).isCloseTo(expected, offset(1e-6));
		assertThat(result.optimizedDistanceMeters()).isCloseTo(expected, offset(1e-6));
		assertThat(result.stops()).hasSize(1);
	}

	@Test
	void twoVisitsCanOnlyGetBetterOrStaySame() {
		Visit first = new Visit(1, new GeoPoint(43.05, -7.6), null, null);
		Visit second = new Visit(2, new GeoPoint(42.9, -7.4), null, null);

		OptimizationResult result = optimizer.optimize(HOME, List.of(first, second));

		assertThat(result.optimizedDistanceMeters()).isLessThanOrEqualTo(result.naiveDistanceMeters());
		assertThat(result.stops()).hasSize(2);
	}
}
