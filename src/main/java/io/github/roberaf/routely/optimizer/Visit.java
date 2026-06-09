package io.github.roberaf.routely.optimizer;

import java.time.LocalTime;

/**
 * A stop the optimizer needs to place on the route, with an optional delivery
 * time window. The web layer maps a {@code Customer} entity to this before
 * calling into the optimizer, and maps the result back.
 */
public record Visit(long customerId, GeoPoint location, LocalTime windowOpen, LocalTime windowClose) {

	public Visit {
		if (location == null) {
			throw new IllegalArgumentException("location must not be null");
		}
		boolean openSet = windowOpen != null;
		boolean closeSet = windowClose != null;
		if (openSet != closeSet) {
			throw new IllegalArgumentException("windowOpen and windowClose must both be set or both be null");
		}
		if (openSet && !windowOpen.isBefore(windowClose)) {
			throw new IllegalArgumentException("windowOpen must be strictly before windowClose");
		}
	}

	public boolean hasTimeWindow() {
		return windowOpen != null;
	}
}
