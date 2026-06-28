package io.github.roberaf.routely.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.roberaf.routely.api.dto.ComputeRouteRequest;
import io.github.roberaf.routely.domain.Customer;
import io.github.roberaf.routely.domain.CustomerPriority;
import io.github.roberaf.routely.domain.Points;
import io.github.roberaf.routely.domain.RoutePlan;
import io.github.roberaf.routely.domain.SalesRep;
import io.github.roberaf.routely.optimizer.RouteOptimizer;
import io.github.roberaf.routely.repository.CustomerRepository;
import io.github.roberaf.routely.repository.RoutePlanRepository;
import io.github.roberaf.routely.repository.SalesRepRepository;

/**
 * Wires a real {@link RouteOptimizer} against mocked repositories, so these
 * tests cover the glue between the persistence model and the pure-Java
 * optimizer without needing a database.
 */
@ExtendWith(MockitoExtension.class)
class RoutePlanServiceTest {

	@Mock
	private SalesRepRepository salesRepRepository;
	@Mock
	private CustomerRepository customerRepository;
	@Mock
	private RoutePlanRepository routePlanRepository;

	private RoutePlanService routePlanService;

	private SalesRep rep;
	private Customer customerA;
	private Customer customerB;
	private Customer customerC;

	@BeforeEach
	void setUp() {
		routePlanService = new RoutePlanService(salesRepRepository, customerRepository, routePlanRepository,
				new RouteOptimizer());

		rep = new SalesRep("Brais", "brais@routely.dev", Points.of(43.0121, -7.5559));
		ReflectionTestUtils.setField(rep, "id", 1L);

		customerA = customerWithId(10L, "A Coruña HQ", 43.3623, -8.4115);
		customerB = customerWithId(11L, "Ferrol depot", 43.4832, -8.2369);
		customerC = customerWithId(12L, "Betanzos shop", 43.2803, -8.2136);
	}

	private static Customer customerWithId(long id, String name, double lat, double lng) {
		Customer customer = Customer.create(name, "address for " + name, Points.of(lat, lng),
				CustomerPriority.NORMAL, null, null);
		ReflectionTestUtils.setField(customer, "id", id);
		return customer;
	}

	@Test
	void computePersistsPlanWithContiguousSequenceAndImprovedDistance() {
		when(salesRepRepository.findById(1L)).thenReturn(Optional.of(rep));
		when(customerRepository.findByActiveTrue()).thenReturn(List.of(customerA, customerB, customerC));
		when(routePlanRepository.save(any(RoutePlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ComputeRouteRequest request = new ComputeRouteRequest(1L, LocalDate.of(2026, 7, 20), null);
		RoutePlan plan = routePlanService.compute(request);

		assertThat(plan.getSalesRep()).isEqualTo(rep);
		assertThat(plan.getComputedAt()).isNotNull();
		assertThat(plan.getStops()).hasSize(3);
		assertThat(plan.getTotalDistanceMeters()).isLessThanOrEqualTo(plan.getNaiveDistanceMeters());

		List<Long> orderedIds = plan.getStops().stream().map(stop -> stop.getCustomer().getId()).toList();
		assertThat(orderedIds).containsExactlyInAnyOrder(10L, 11L, 12L);

		for (int i = 0; i < plan.getStops().size(); i++) {
			assertThat(plan.getStops().get(i).getSequenceIndex()).isEqualTo(i);
		}
	}

	@Test
	void computeWithExplicitMissingCustomerIdThrowsNotFound() {
		when(salesRepRepository.findById(1L)).thenReturn(Optional.of(rep));
		when(customerRepository.findAllById(List.of(10L, 999L))).thenReturn(List.of(customerA));

		ComputeRouteRequest request = new ComputeRouteRequest(1L, LocalDate.of(2026, 7, 20), List.of(10L, 999L));

		assertThatThrownBy(() -> routePlanService.compute(request))
				.isInstanceOf(NotFoundException.class)
				.hasMessageContaining("999");
	}

	@Test
	void computeWithInactiveCustomerIdThrowsIllegalArgument() {
		customerB.setActive(false);
		when(salesRepRepository.findById(1L)).thenReturn(Optional.of(rep));
		when(customerRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(customerA, customerB));

		ComputeRouteRequest request = new ComputeRouteRequest(1L, LocalDate.of(2026, 7, 20), List.of(10L, 11L));

		assertThatThrownBy(() -> routePlanService.compute(request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("11");
	}

	@Test
	void computeWithNoActiveCustomersThrowsIllegalArgument() {
		when(salesRepRepository.findById(1L)).thenReturn(Optional.of(rep));
		when(customerRepository.findByActiveTrue()).thenReturn(List.of());

		ComputeRouteRequest request = new ComputeRouteRequest(1L, LocalDate.of(2026, 7, 20), null);

		assertThatThrownBy(() -> routePlanService.compute(request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("no active customers");
	}

	@Test
	void computeWithUnknownRepThrowsNotFound() {
		when(salesRepRepository.findById(42L)).thenReturn(Optional.empty());

		ComputeRouteRequest request = new ComputeRouteRequest(42L, LocalDate.of(2026, 7, 20), null);

		assertThatThrownBy(() -> routePlanService.compute(request))
				.isInstanceOf(NotFoundException.class)
				.hasMessageContaining("42");
	}
}
