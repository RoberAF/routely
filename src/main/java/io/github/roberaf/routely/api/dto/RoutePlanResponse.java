package io.github.roberaf.routely.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import io.github.roberaf.routely.domain.Customer;
import io.github.roberaf.routely.domain.RoutePlan;
import io.github.roberaf.routely.domain.RouteStop;

public record RoutePlanResponse(
		long id,
		long repId,
		LocalDate planDate,
		double totalDistanceMeters,
		double naiveDistanceMeters,
		double improvementPercent,
		int timeWindowViolations,
		Instant computedAt,
		List<RouteStopResponse> stops) {

	public static RoutePlanResponse from(RoutePlan plan) {
		double naive = plan.getNaiveDistanceMeters();
		double total = plan.getTotalDistanceMeters();
		double improvementPercent = naive <= 0 ? 0 : (naive - total) / naive * 100;

		List<RouteStopResponse> stops = plan.getStops().stream().map(RoutePlanResponse::toStopResponse).toList();
		int violations = (int) plan.getStops().stream().filter(RoutePlanResponse::violatesWindow).count();

		return new RoutePlanResponse(
				plan.getId(),
				plan.getSalesRep().getId(),
				plan.getPlanDate(),
				total,
				naive,
				improvementPercent,
				violations,
				plan.getComputedAt(),
				stops);
	}

	private static RouteStopResponse toStopResponse(RouteStop stop) {
		Customer customer = stop.getCustomer();
		return new RouteStopResponse(
				customer.getId(),
				customer.getName(),
				stop.getSequenceIndex(),
				customer.latitude(),
				customer.longitude(),
				stop.getEstimatedArrival());
	}

	/**
	 * Recomputed from the persisted stop rather than trusted from the
	 * optimizer run: a stop is late if its window has a close time and the
	 * estimated arrival falls after it. The construction step in the
	 * optimizer never schedules an arrival before a window opens (it waits),
	 * so a violation can only ever show up as arriving too late.
	 */
	private static boolean violatesWindow(RouteStop stop) {
		Customer customer = stop.getCustomer();
		LocalTime close = customer.getTimeWindowClose();
		LocalTime arrival = stop.getEstimatedArrival();
		return close != null && arrival != null && arrival.isAfter(close);
	}
}
