package io.github.roberaf.routely.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import io.github.roberaf.routely.domain.Customer;
import io.github.roberaf.routely.domain.CustomerPriority;
import io.github.roberaf.routely.domain.Points;
import io.github.roberaf.routely.security.JwtService;
import io.github.roberaf.routely.security.SecurityConfig;
import io.github.roberaf.routely.service.CustomerService;
import io.github.roberaf.routely.service.NotFoundException;

@WebMvcTest(controllers = CustomerController.class)
@Import({ SecurityConfig.class, JwtService.class, ApiExceptionHandler.class })
class CustomerControllerTest {

	private static final String CUSTOMERS_URL = "/api/v1/customers";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CustomerService customerService;

	private static JwtRequestPostProcessor manager() {
		return jwt().jwt(j -> j.claim("role", "MANAGER")).authorities(new SimpleGrantedAuthority("ROLE_MANAGER"));
	}

	private static JwtRequestPostProcessor rep() {
		return jwt().jwt(j -> j.claim("role", "REP")).authorities(new SimpleGrantedAuthority("ROLE_REP"));
	}

	private static Customer customerWithId(long id, String name) {
		Customer customer = Customer.create(name, "Rua Real " + id, Points.of(43.36, -8.41), CustomerPriority.NORMAL,
				null, null);
		ReflectionTestUtils.setField(customer, "id", id);
		return customer;
	}

	@Test
	void postAsManagerCreatesAndReturns201WithLocation() throws Exception {
		when(customerService.create(any())).thenReturn(customerWithId(5L, "A Coruña client"));

		mockMvc.perform(post(CUSTOMERS_URL)
						.with(manager())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"A Coruña client","address":"Rua Real 1","lat":43.36,"lng":-8.41,"priority":"NORMAL"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/v1/customers/5"))
				.andExpect(jsonPath("$.id").value(5))
				.andExpect(jsonPath("$.name").value("A Coruña client"))
				.andExpect(jsonPath("$.active").value(true));
	}

	@Test
	void postAsRepIsForbidden() throws Exception {
		mockMvc.perform(post(CUSTOMERS_URL)
						.with(rep())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"A Coruña client","address":"Rua Real 1","lat":43.36,"lng":-8.41,"priority":"NORMAL"}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(content().contentType("application/problem+json"));
	}

	@Test
	void postWithLatOutOfRangeIs400WithErrorsProperty() throws Exception {
		mockMvc.perform(post(CUSTOMERS_URL)
						.with(manager())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Bad customer","address":"Rua Real 1","lat":999,"lng":-8.41,"priority":"NORMAL"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType("application/problem+json"))
				.andExpect(jsonPath("$.title").value("Validation failed"))
				.andExpect(jsonPath("$.errors.lat").exists());
	}

	@Test
	void getByIdUnknownReturns404ProblemJson() throws Exception {
		when(customerService.byId(42L)).thenThrow(new NotFoundException("customer 42 not found"));

		mockMvc.perform(get(CUSTOMERS_URL + "/42").with(rep()))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType("application/problem+json"))
				.andExpect(jsonPath("$.title").value("Not found"))
				.andExpect(jsonPath("$.detail").value("customer 42 not found"));
	}

	@Test
	void getNearbyWithNegativeRadiusIs400() throws Exception {
		mockMvc.perform(get(CUSTOMERS_URL + "/nearby")
						.param("lat", "43.36")
						.param("lng", "-8.41")
						.param("radiusMeters", "-5")
						.with(rep()))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType("application/problem+json"));
	}

	@Test
	void getListReturnsPageResponseShape() throws Exception {
		Customer customer = customerWithId(7L, "Lugo client");
		when(customerService.page(0, 20)).thenReturn(new PageImpl<>(List.of(customer), PageRequest.of(0, 20), 1));

		mockMvc.perform(get(CUSTOMERS_URL).with(rep()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(7))
				.andExpect(jsonPath("$.content[0].name").value("Lugo client"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1));
	}
}
