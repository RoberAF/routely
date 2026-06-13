package io.github.roberaf.routely.optimizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Classic 2-opt local search over the round trip: repeatedly try reversing a
 * contiguous segment of the order and keep the reversal if it both shortens the
 * round trip and does not make the schedule's time-window violations worse.
 */
public final class TwoOpt {

	private static final double MIN_IMPROVEMENT_METERS = 1e-6;

	private TwoOpt() {
	}

	public static List<Visit> improve(GeoPoint home, List<Visit> order, int maxPasses) {
		return improveTracked(home, order, maxPasses).order();
	}

	/**
	 * Same algorithm as {@link #improve}, also reporting how many passes actually
	 * ran before the loop converged or hit {@code maxPasses}, for
	 * {@link RouteOptimizer} to surface in {@link OptimizationResult}.
	 */
	static Result improveTracked(GeoPoint home, List<Visit> order, int maxPasses) {
		List<Visit> current = new ArrayList<>(order);
		int size = current.size();

		double currentDistance = TourMath.roundTripMeters(home, current);
		int currentViolations = ScheduleEstimator.countViolations(ScheduleEstimator.schedule(home, current));

		int passesRun = 0;
		for (int pass = 0; pass < maxPasses; pass++) {
			passesRun++;
			boolean improvedThisPass = false;

			for (int i = 0; i < size - 1; i++) {
				for (int j = i + 1; j < size; j++) {
					List<Visit> candidate = withSegmentReversed(current, i, j);
					double candidateDistance = TourMath.roundTripMeters(home, candidate);
					if (candidateDistance >= currentDistance - MIN_IMPROVEMENT_METERS) {
						continue;
					}
					int candidateViolations = ScheduleEstimator
							.countViolations(ScheduleEstimator.schedule(home, candidate));
					if (candidateViolations > currentViolations) {
						continue;
					}
					current = candidate;
					currentDistance = candidateDistance;
					currentViolations = candidateViolations;
					improvedThisPass = true;
				}
			}

			if (!improvedThisPass) {
				break;
			}
		}
		return new Result(current, passesRun);
	}

	private static List<Visit> withSegmentReversed(List<Visit> order, int fromIndex, int toIndex) {
		List<Visit> result = new ArrayList<>(order);
		int lo = fromIndex;
		int hi = toIndex;
		while (lo < hi) {
			Visit tmp = result.get(lo);
			result.set(lo, result.get(hi));
			result.set(hi, tmp);
			lo++;
			hi--;
		}
		return result;
	}

	record Result(List<Visit> order, int passesRun) {
	}
}
