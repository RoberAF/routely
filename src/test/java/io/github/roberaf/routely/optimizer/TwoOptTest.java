package io.github.roberaf.routely.optimizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.Test;

class TwoOptTest {

	private static final GeoPoint HOME = new GeoPoint(0, 0);

	// A square-ish walk where the naive/crossed order makes the tour's edges cross itself.
	private static final Visit A = new Visit(1, new GeoPoint(0, 0.2), null, null);
	private static final Visit B = new Visit(2, new GeoPoint(0.15, 0.2), null, null);
	private static final Visit C = new Visit(3, new GeoPoint(0.15, 0), null, null);

	@Test
	void uncrossesTheKnownCrossingFixture() {
		List<Visit> crossed = List.of(A, C, B);
		double initialDistance = TourMath.roundTripMeters(HOME, crossed);
		assertThat(initialDistance).isCloseTo(100_075.0, within(50.0));

		List<Visit> improved = TwoOpt.improve(HOME, crossed, RouteOptimizer.MAX_TWO_OPT_PASSES);
		double improvedDistance = TourMath.roundTripMeters(HOME, improved);

		assertThat(improvedDistance).isLessThanOrEqualTo(77_900.0);
		assertThat(improvedDistance).isLessThan(initialDistance);
		List<Long> customerIds = improved.stream().map(Visit::customerId).toList();
		boolean isEitherDirectionOfTheSquare = customerIds.equals(List.of(1L, 2L, 3L))
				|| customerIds.equals(List.of(3L, 2L, 1L));
		assertThat(isEitherDirectionOfTheSquare).isTrue();
	}

	@Test
	void cannotImproveAnAlreadyOptimalColinearOrder() {
		Visit near = new Visit(10, new GeoPoint(0, 0.01), null, null);
		Visit middle = new Visit(20, new GeoPoint(0, 0.02), null, null);
		Visit far = new Visit(30, new GeoPoint(0, 0.03), null, null);

		List<Visit> nnOrder = NearestNeighbor.buildOrder(HOME, List.of(far, near, middle));
		double nnDistance = TourMath.roundTripMeters(HOME, nnOrder);

		List<Visit> improved = TwoOpt.improve(HOME, nnOrder, RouteOptimizer.MAX_TWO_OPT_PASSES);
		double improvedDistance = TourMath.roundTripMeters(HOME, improved);

		assertThat(improvedDistance).isEqualTo(nnDistance);
	}

	@Test
	void routeOptimizerImprovesOnADeliberatelyBadInputOrder() {
		List<Visit> badOrder = List.of(A, C, B);

		OptimizationResult result = new RouteOptimizer().optimize(HOME, badOrder);

		assertThat(result.naiveDistanceMeters()).isGreaterThan(result.optimizedDistanceMeters());
		assertThat(result.improvementPercent()).isGreaterThan(0);
	}
}
