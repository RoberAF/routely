package io.github.roberaf.routely.api.dto;

import java.time.LocalTime;

public record RouteStopResponse(
		long customerId,
		String customerName,
		int sequenceIndex,
		double lat,
		double lng,
		LocalTime estimatedArrival) {
}
