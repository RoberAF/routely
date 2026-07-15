package io.github.roberaf.routely.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.roberaf.routely.api.dto.ComputeRouteRequest;
import io.github.roberaf.routely.domain.Customer;
import io.github.roberaf.routely.domain.RoutePlan;
import io.github.roberaf.routely.domain.SalesRep;
import io.github.roberaf.routely.optimizer.GeoPoint;
import io.github.roberaf.routely.optimizer.OptimizationResult;
import io.github.roberaf.routely.optimizer.RouteOptimizer;
import io.github.roberaf.routely.optimizer.ScheduledVisit;
import io.github.roberaf.routely.optimizer.Visit;
import io.github.roberaf.routely.repository.CustomerRepository;
import io.github.roberaf.routely.repository.RoutePlanRepository;
import io.github.roberaf.routely.repository.SalesRepRepository;

@Service
public class RoutePlanService {

	private final SalesRepRepository salesRepRepository;
	private final CustomerRepository customerRepository;
	private final RoutePlanRepository routePlanRepository;
	private final RouteOptimizer routeOptimizer;

	public RoutePlanService(SalesRepRepository salesRepRepository, CustomerRepository customerRepository,
			RoutePlanRepository routePlanRepository, RouteOptimizer routeOptimizer) {
		this.salesRepRepository = salesRepRepository;
		this.customerRepository = customerRepository;
		this.routePlanRepository = routePlanRepository;
		this.routeOptimizer = routeOptimizer;
	}

	@Transactional
	public RoutePlan compute(ComputeRouteRequest request) {
		SalesRep rep = salesRepRepository.findById(request.repId())
				.orElseThrow(() -> new NotFoundException("sales rep " + request.repId() + " not found"));

		List<Customer> customers = resolveCustomers(request.customerIds());
		if (customers.isEmpty()) {
			throw new IllegalArgumentException("no active customers to route");
		}

		Map<Long, Customer> customersById = new HashMap<>();
		List<Visit> visits = new ArrayList<>(customers.size());
		for (Customer customer : customers) {
			customersById.put(customer.getId(), customer);
			visits.add(new Visit(customer.getId(), new GeoPoint(customer.latitude(), customer.longitude()),
					customer.getTimeWindowOpen(), customer.getTimeWindowClose()));
		}

		GeoPoint homeBase = new GeoPoint(rep.latitude(), rep.longitude());
		OptimizationResult result = routeOptimizer.optimize(homeBase, visits);

		RoutePlan plan = new RoutePlan(rep, request.planDate());
		plan.setTotalDistanceMeters(result.optimizedDistanceMeters());
		plan.setNaiveDistanceMeters(result.naiveDistanceMeters());
		plan.setComputedAt(Instant.now());
		for (ScheduledVisit scheduledVisit : result.stops()) {
			Customer customer = customersById.get(scheduledVisit.visit().customerId());
			plan.addStop(customer, scheduledVisit.estimatedArrival());
		}

		return routePlanRepository.save(plan);
	}

	public RoutePlan byId(long id) {
		return routePlanRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("route plan " + id + " not found"));
	}

	public List<RoutePlan> plansFor(Long repId, LocalDate planDate) {
		if (planDate != null) {
			return routePlanRepository.findBySalesRepIdAndPlanDate(repId, planDate);
		}
		return routePlanRepository.findBySalesRepId(repId);
	}

	private List<Customer> resolveCustomers(List<Long> customerIds) {
		if (customerIds == null || customerIds.isEmpty()) {
			return customerRepository.findByActiveTrue();
		}

		Map<Long, Customer> found = new HashMap<>();
		for (Customer customer : customerRepository.findAllById(customerIds)) {
			found.put(customer.getId(), customer);
		}

		List<Long> missing = new ArrayList<>();
		for (Long id : customerIds) {
			if (!found.containsKey(id)) {
				missing.add(id);
			}
		}
		if (!missing.isEmpty()) {
			throw new NotFoundException("customers not found: " + missing);
		}

		List<Customer> ordered = new ArrayList<>(customerIds.size());
		for (Long id : customerIds) {
			Customer customer = found.get(id);
			if (!customer.isActive()) {
				throw new IllegalArgumentException("customer " + id + " is inactive");
			}
			ordered.add(customer);
		}
		return ordered;
	}
}
