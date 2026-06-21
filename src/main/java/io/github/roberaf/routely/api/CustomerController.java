package io.github.roberaf.routely.api;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.roberaf.routely.api.dto.CustomerCreateRequest;
import io.github.roberaf.routely.api.dto.CustomerResponse;
import io.github.roberaf.routely.api.dto.PageResponse;
import io.github.roberaf.routely.domain.Customer;
import io.github.roberaf.routely.service.CustomerService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

	private static final int MAX_PAGE_SIZE = 100;
	private static final double MAX_NEARBY_RADIUS_METERS = 50_000;

	private final CustomerService customerService;

	public CustomerController(CustomerService customerService) {
		this.customerService = customerService;
	}

	@PostMapping
	public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerCreateRequest request) {
		Customer customer = customerService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/customers/" + customer.getId()))
				.body(CustomerResponse.from(customer));
	}

	@GetMapping
	public PageResponse<CustomerResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		Page<Customer> result = customerService.page(page, Math.min(size, MAX_PAGE_SIZE));
		return PageResponse.of(result, CustomerResponse::from);
	}

	@GetMapping("/{id}")
	public CustomerResponse byId(@PathVariable long id) {
		return CustomerResponse.from(customerService.byId(id));
	}

	@GetMapping("/nearby")
	public List<CustomerResponse> nearby(
			@RequestParam double lat,
			@RequestParam double lng,
			@RequestParam double radiusMeters) {
		if (lat < -90 || lat > 90) {
			throw new IllegalArgumentException("lat must be in [-90, 90]");
		}
		if (lng < -180 || lng > 180) {
			throw new IllegalArgumentException("lng must be in [-180, 180]");
		}
		if (radiusMeters <= 0 || radiusMeters > MAX_NEARBY_RADIUS_METERS) {
			throw new IllegalArgumentException("radiusMeters must be > 0 and <= " + MAX_NEARBY_RADIUS_METERS);
		}
		return customerService.nearby(lat, lng, radiusMeters).stream().map(CustomerResponse::from).toList();
	}
}
