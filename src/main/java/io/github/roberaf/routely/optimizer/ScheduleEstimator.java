package io.github.roberaf.routely.optimizer;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Simulates arrival times for a fixed visit order, departing from a home base at
 * {@link RouteOptimizer#DEPARTURE_TIME}.
 *
 * <p><strong>Known limitation:</strong> this uses {@link LocalTime} arithmetic,
 * which has no concept of a calendar date and simply wraps at midnight. A route
 * whose cumulative travel, waiting and service time pushes past 24:00 from the
 * departure time will wrap around to an early morning clock value instead of
 * rolling over to the next day - this estimator is only meant for single-day
 * routes that comfortably fit in a working day.
 */
public final class ScheduleEstimator {

	private ScheduleEstimator() {
	}

	public static List<ScheduledVisit> schedule(GeoPoint home, List<Visit> order) {
		List<ScheduledVisit> stops = new ArrayList<>(order.size());
		GeoPoint currentPosition = home;
		LocalTime currentClock = RouteOptimizer.DEPARTURE_TIME;
		for (Visit visit : order) {
			Arrival arrival = arrive(currentPosition, currentClock, visit);
			stops.add(new ScheduledVisit(visit, arrival.effectiveArrival(), arrival.withinWindow()));
			currentPosition = visit.location();
			currentClock = arrival.effectiveArrival().plus(RouteOptimizer.SERVICE_TIME);
		}
		return stops;
	}

	public static int countViolations(List<ScheduledVisit> stops) {
		int count = 0;
		for (ScheduledVisit stop : stops) {
			if (!stop.withinWindow()) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Works out when a visit is actually reached (and whether that honors its
	 * window) given a departure position/clock. Shared by {@link #schedule} and
	 * {@link NearestNeighbor} so both use exactly the same arrival rules.
	 */
	static Arrival arrive(GeoPoint fromPosition, LocalTime fromClock, Visit visit) {
		double distanceMeters = Haversine.distanceMeters(fromPosition, visit.location());
		double travelSeconds = distanceMeters / RouteOptimizer.AVERAGE_SPEED_MPS;
		long travelNanos = Math.round(travelSeconds * 1_000_000_000.0);
		LocalTime rawArrival = fromClock.plusNanos(travelNanos);

		if (!visit.hasTimeWindow()) {
			return new Arrival(rawArrival, true);
		}
		if (rawArrival.isBefore(visit.windowOpen())) {
			return new Arrival(visit.windowOpen(), true);
		}
		boolean withinWindow = !rawArrival.isAfter(visit.windowClose());
		return new Arrival(rawArrival, withinWindow);
	}

	record Arrival(LocalTime effectiveArrival, boolean withinWindow) {
	}
}
