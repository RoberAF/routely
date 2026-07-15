package io.github.roberaf.routely.optimizer;

import java.util.List;

/**
 * Distance calculations over an ordered list of visits.
 */
public final class TourMath {

	private TourMath() {
	}

	/**
	 * Total great-circle distance of the round trip home -> visits in order -> home.
	 * Zero for an empty list (never leaving home).
	 */
	public static double roundTripMeters(GeoPoint home, List<Visit> order) {
		if (order.isEmpty()) {
			return 0;
		}
		double total = 0;
		GeoPoint current = home;
		for (Visit visit : order) {
			total += Haversine.distanceMeters(current, visit.location());
			current = visit.location();
		}
		total += Haversine.distanceMeters(current, home);
		return total;
	}
}
