package io.github.roberaf.routely.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import io.github.roberaf.routely.domain.AppUser;
import io.github.roberaf.routely.domain.Role;
import io.github.roberaf.routely.domain.SalesRep;
import io.github.roberaf.routely.repository.AppUserRepository;

/**
 * Only AuthController lives in this @WebMvcTest slice, so every protected
 * URL below has no handler behind the security filters: a request that
 * clears authentication and authorization falls through to Spring's 404,
 * which is exactly what tells these tests the filter chain let it pass.
 */
@WebMvcTest(controllers = AuthController.class)
@Import({ SecurityConfig.class, JwtService.class, AuthExceptionHandler.class })
class SecurityRulesTest {

	private static final String CUSTOMERS_URL = "/api/v1/customers";
	private static final String ROUTE_PLANS_COMPUTE_URL = "/api/v1/route-plans/compute";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@MockitoBean
	private AppUserRepository appUserRepository;

	private static AppUser userWithId(long id, String email, Role role, SalesRep salesRep) {
		AppUser user = new AppUser(email, "irrelevant-hash", role, salesRep);
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private static SalesRep salesRepWithId(long id) {
		SalesRep salesRep = new SalesRep("Brais", "brais@routely.dev", null);
		ReflectionTestUtils.setField(salesRep, "id", id);
		return salesRep;
	}

	private String repToken() {
		return jwtService.issueToken(userWithId(2L, "brais@routely.dev", Role.REP, salesRepWithId(4L)));
	}

	private String managerToken() {
		return jwtService.issueToken(userWithId(1L, "manager@routely.dev", Role.MANAGER, null));
	}

	private String adminToken() {
		return jwtService.issueToken(userWithId(1L, "admin@routely.dev", Role.ADMIN, null));
	}

	@Test
	void getCustomersWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get(CUSTOMERS_URL))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType("application/problem+json"));
	}

	@Test
	void getCustomersWithRepTokenPassesAuthAndFallsThroughTo404() throws Exception {
		mockMvc.perform(get(CUSTOMERS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + repToken()))
				.andExpect(status().isNotFound());
	}

	@Test
	void postCustomersWithRepTokenIsForbidden() throws Exception {
		mockMvc.perform(post(CUSTOMERS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + repToken()))
				.andExpect(status().isForbidden())
				.andExpect(content().contentType("application/problem+json"));
	}

	@Test
	void postCustomersWithManagerTokenPassesAuthAndFallsThroughTo404() throws Exception {
		mockMvc.perform(post(CUSTOMERS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken()))
				.andExpect(status().isNotFound());
	}

	@Test
	void postRoutePlansComputeWithRepTokenIsForbidden() throws Exception {
		mockMvc.perform(post(ROUTE_PLANS_COMPUTE_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + repToken()))
				.andExpect(status().isForbidden())
				.andExpect(content().contentType("application/problem+json"));
	}

	@Test
	void postRoutePlansComputeWithAdminTokenPassesAuthAndFallsThroughTo404() throws Exception {
		mockMvc.perform(post(ROUTE_PLANS_COMPUTE_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
				.andExpect(status().isNotFound());
	}

	@Test
	void garbageTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get(CUSTOMERS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer nonsense"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType("application/problem+json"));
	}
}
