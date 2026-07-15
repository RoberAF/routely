package io.github.roberaf.routely.optimizer;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

/**
 * Entry point for the route optimization heuristic.
 *
 * <p><strong>This is a heuristic, not an exact solver.</strong> It builds an
 * initial tour with a time-window-aware nearest-neighbor construction
 * ({@link NearestNeighbor}), then improves it with a bounded number of 2-opt
 * passes ({@link TwoOpt}). For anything beyond a handful of stops this will not
 * find the true optimum - it trades solution quality for being fast and simple
 * enough to run synchronously on every request.
 *
 * <p>Specifically:
 * <ul>
 *   <li><strong>Time windows are handled greedily, not guaranteed.</strong> The
 *   nearest-neighbor construction prefers a feasible visit (one that can still be
 *   reached before its window closes) over a merely closer one, and 2-opt only
 *   accepts a move if it does not increase the number of window violations
 *   compared to the current order. Neither step ever rolls back a stop that is
 *   already unreachable in time - violations are reported via
 *   {@link OptimizationResult#timeWindowViolations()}, not eliminated. A
 *   sufficiently constrained input (too many tight windows for one route) will
 *   come back with a nonzero violation count.</li>
 *   <li><strong>Distances are great-circle, not road distances.</strong>
 *   {@link Haversine} gives straight-line distance between coordinates; it knows
 *   nothing about roads, one-ways, rivers, or traffic, so real travel distance
 *   (and therefore real travel time) will generally be longer than what this
 *   class reports, especially in dense urban areas.</li>
 *   <li><strong>ETAs use a single fixed average speed and fixed service time.</strong>
 *   Every leg of the trip is assumed to be driven at {@link #AVERAGE_SPEED_MPS}
 *   with exactly {@link #SERVICE_TIME} spent at each stop - no rush hour, no stop
 *   taking longer than another.</li>
 * </ul>
 */
public final class RouteOptimizer {

	public static final int MAX_TWO_OPT_PASSES = 50;
	public static final LocalTime DEPARTURE_TIME = LocalTime.of(9, 0);
	public static final double AVERAGE_SPEED_MPS = 50_000.0 / 3600.0;
	public static final Duration SERVICE_TIME = Duration.ofMinutes(15);

	public OptimizationResult optimize(GeoPoint homeBase, List<Visit> visits) {
		double naiveDistance = TourMath.roundTripMeters(homeBase, visits);

		List<Visit> nearestNeighborOrder = NearestNeighbor.buildOrder(homeBase, visits);
		double nearestNeighborDistance = TourMath.roundTripMeters(homeBase, nearestNeighborOrder);

		TwoOpt.Result twoOptResult = TwoOpt.improveTracked(homeBase, nearestNeighborOrder, MAX_TWO_OPT_PASSES);
		double optimizedDistance = TourMath.roundTripMeters(homeBase, twoOptResult.order());

		List<ScheduledVisit> stops = ScheduleEstimator.schedule(homeBase, twoOptResult.order());
		int violations = ScheduleEstimator.countViolations(stops);

		return new OptimizationResult(stops, naiveDistance, nearestNeighborDistance, optimizedDistance,
				twoOptResult.passesRun(), violations);
	}
}
