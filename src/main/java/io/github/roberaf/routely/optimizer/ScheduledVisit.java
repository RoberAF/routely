package io.github.roberaf.routely.optimizer;

import java.time.LocalTime;

/**
 * A {@link Visit} placed at a point in the route, with its estimated arrival
 * (after any window-opening wait) and whether that arrival honors the visit's
 * time window.
 */
public record ScheduledVisit(Visit visit, LocalTime estimatedArrival, boolean withinWindow) {
}
