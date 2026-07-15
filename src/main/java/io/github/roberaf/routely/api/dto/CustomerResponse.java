package io.github.roberaf.routely.api.dto;

import java.time.LocalTime;

import io.github.roberaf.routely.domain.Customer;
import io.github.roberaf.routely.domain.CustomerPriority;

public record CustomerResponse(
		long id,
		String name,
		String address,
		double lat,
		double lng,
		CustomerPriority priority,
		LocalTime timeWindowOpen,
		LocalTime timeWindowClose,
		boolean active) {

	public static CustomerResponse from(Customer customer) {
		return new CustomerResponse(
				customer.getId(),
				customer.getName(),
				customer.getAddress(),
				customer.latitude(),
				customer.longitude(),
				customer.getPriority(),
				customer.getTimeWindowOpen(),
				customer.getTimeWindowClose(),
				customer.isActive());
	}
}
