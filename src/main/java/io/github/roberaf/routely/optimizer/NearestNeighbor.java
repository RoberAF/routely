package io.github.roberaf.routely.optimizer;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Greedy, time-window-aware nearest-neighbor tour construction: at each step,
 * among the still-unvisited stops, prefer the nearest one that can still be
 * reached before its window closes; fall back to the nearest overall if none of
 * the remaining stops are reachable in time.
 */
public final class NearestNeighbor {

	private NearestNeighbor() {
	}

	public static List<Visit> buildOrder(GeoPoint home, List<Visit> visits) {
		List<Visit> unvisited = new ArrayList<>(visits);
		List<Visit> order = new ArrayList<>(visits.size());

		GeoPoint currentPosition = home;
		LocalTime currentClock = RouteOptimizer.DEPARTURE_TIME;

		while (!unvisited.isEmpty()) {
			Visit next = pickNext(currentPosition, currentClock, unvisited);
			order.add(next);
			unvisited.remove(next);

			ScheduleEstimator.Arrival arrival = ScheduleEstimator.arrive(currentPosition, currentClock, next);
			currentPosition = next.location();
			currentClock = arrival.effectiveArrival().plus(RouteOptimizer.SERVICE_TIME);
		}
		return order;
	}

	private static Visit pickNext(GeoPoint currentPosition, LocalTime currentClock, List<Visit> unvisited) {
		Visit bestFeasible = null;
		double bestFeasibleDistance = Double.MAX_VALUE;
		Visit bestOverall = null;
		double bestOverallDistance = Double.MAX_VALUE;

		for (Visit candidate : unvisited) {
			double distance = Haversine.distanceMeters(currentPosition, candidate.location());

			if (isCloserOrTieBreaks(distance, candidate, bestOverallDistance, bestOverall)) {
				bestOverall = candidate;
				bestOverallDistance = distance;
			}
			if (isFeasible(currentPosition, currentClock, candidate)
					&& isCloserOrTieBreaks(distance, candidate, bestFeasibleDistance, bestFeasible)) {
				bestFeasible = candidate;
				bestFeasibleDistance = distance;
			}
		}
		return bestFeasible != null ? bestFeasible : bestOverall;
	}

	private static boolean isFeasible(GeoPoint currentPosition, LocalTime currentClock, Visit candidate) {
		if (!candidate.hasTimeWindow()) {
			return true;
		}
		return ScheduleEstimator.arrive(currentPosition, currentClock, candidate).withinWindow();
	}

	/** Nearer wins; on an exact distance tie the lower customerId wins, for determinism. */
	private static boolean isCloserOrTieBreaks(double distance, Visit candidate, double bestDistance, Visit best) {
		if (best == null) {
			return true;
		}
		double delta = distance - bestDistance;
		if (delta < -1e-9) {
			return true;
		}
		if (delta > 1e-9) {
			return false;
		}
		return candidate.customerId() < best.customerId();
	}
}
