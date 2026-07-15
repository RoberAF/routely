package io.github.roberaf.routely.optimizer;

import java.util.List;

/**
 * Output of {@link RouteOptimizer#optimize}: the scheduled stops in their final
 * order plus the distance figures at each stage of the heuristic, so callers can
 * see how much the optimization actually helped.
 */
public record OptimizationResult(
		List<ScheduledVisit> stops,
		double naiveDistanceMeters,
		double nearestNeighborDistanceMeters,
		double optimizedDistanceMeters,
		int twoOptPasses,
		int timeWindowViolations) {

	public double improvementPercent() {
		if (naiveDistanceMeters <= 0) {
			return 0;
		}
		return (naiveDistanceMeters - optimizedDistanceMeters) / naiveDistanceMeters * 100;
	}
}
