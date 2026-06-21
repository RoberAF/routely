package io.github.roberaf.routely.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import io.github.roberaf.routely.api.dto.CustomerCreateRequest;
import io.github.roberaf.routely.domain.Customer;
import io.github.roberaf.routely.domain.Points;
import io.github.roberaf.routely.repository.CustomerRepository;

@Service
public class CustomerService {

	private final CustomerRepository customerRepository;

	public CustomerService(CustomerRepository customerRepository) {
		this.customerRepository = customerRepository;
	}

	public Customer create(CustomerCreateRequest request) {
		Customer customer = Customer.create(
				request.name(),
				request.address(),
				Points.of(request.lat(), request.lng()),
				request.priority(),
				request.timeWindowOpen(),
				request.timeWindowClose());
		return customerRepository.save(customer);
	}

	public Page<Customer> page(int page, int size) {
		return customerRepository.findAll(PageRequest.of(page, size, Sort.by("id")));
	}

	public Customer byId(long id) {
		return customerRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("customer " + id + " not found"));
	}

	public List<Customer> nearby(double lat, double lng, double radiusMeters) {
		return customerRepository.findNearby(lat, lng, radiusMeters);
	}
}
