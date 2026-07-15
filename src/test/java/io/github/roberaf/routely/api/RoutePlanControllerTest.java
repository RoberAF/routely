package io.github.roberaf.routely.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import io.github.roberaf.routely.domain.Customer;
import io.github.roberaf.routely.domain.CustomerPriority;
import io.github.roberaf.routely.domain.Points;
import io.github.roberaf.routely.domain.RoutePlan;
import io.github.roberaf.routely.domain.SalesRep;
import io.github.roberaf.routely.security.JwtService;
import io.github.roberaf.routely.security.SecurityConfig;
import io.github.roberaf.routely.service.RoutePlanService;

@WebMvcTest(controllers = RoutePlanController.class)
@Import({ SecurityConfig.class, JwtService.class, ApiExceptionHandler.class })
class RoutePlanControllerTest {

	private static final String ROUTE_PLANS_URL = "/api/v1/route-plans";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RoutePlanService routePlanService;

	private static JwtRequestPostProcessor manager() {
		return jwt().jwt(j -> j.claim("role", "MANAGER")).authorities(new SimpleGrantedAuthority("ROLE_MANAGER"));
	}

	private static JwtRequestPostProcessor repWithOwnId(long repId) {
		return jwt().jwt(j -> j.claim("role", "REP").claim("repId", repId))
				.authorities(new SimpleGrantedAuthority("ROLE_REP"));
	}

	private static SalesRep repWithId(long id) {
		SalesRep rep = new SalesRep("Brais", "brais@routely.dev", Points.of(43.0121, -7.5559));
		ReflectionTestUtils.setField(rep, "id", id);
		return rep;
	}

	private static RoutePlan planFor(SalesRep rep, long planId) {
		RoutePlan plan = new RoutePlan(rep, LocalDate.of(2026, 7, 20));
		ReflectionTestUtils.setField(plan, "id", planId);
		plan.setNaiveDistanceMeters(1000);
		plan.setTotalDistanceMeters(800);
		plan.setComputedAt(Instant.parse("2026-07-20T09:00:00Z"));

		Customer customer = Customer.create("Customer 10", "Rua Real 10", Points.of(43.36, -8.41),
				CustomerPriority.NORMAL, null, null);
		ReflectionTestUtils.setField(customer, "id", 10L);
		plan.addStop(customer, LocalTime.of(9, 30));
		return plan;
	}

	@Test
	void computeAsManagerReturns201WithStopsAndImprovement() throws Exception {
		RoutePlan plan = planFor(repWithId(1L), 100L);
		when(routePlanService.compute(any())).thenReturn(plan);

		mockMvc.perform(post(ROUTE_PLANS_URL + "/compute")
						.with(manager())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"repId":1,"planDate":"2026-07-20"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/v1/route-plans/100"))
				.andExpect(jsonPath("$.stops[0].customerId").value(10))
				.andExpect(jsonPath("$.stops[0].sequenceIndex").value(0))
				.andExpect(jsonPath("$.improvementPercent").value(20.0));
	}

	@Test
	void computeAsRepIsForbidden() throws Exception {
		mockMvc.perform(post(ROUTE_PLANS_URL + "/compute")
						.with(repWithOwnId(1L))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"repId":1,"planDate":"2026-07-20"}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(content().contentType("application/problem+json"));
	}

	@Test
	void getAsRepWithMatchingRepIdParamReturns200() throws Exception {
		when(routePlanService.plansFor(1L, null)).thenReturn(List.of(planFor(repWithId(1L), 100L)));

		mockMvc.perform(get(ROUTE_PLANS_URL).param("repId", "1").with(repWithOwnId(1L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].repId").value(1));
	}

	@Test
	void getAsRepWithDifferentRepIdParamIsForbidden() throws Exception {
		mockMvc.perform(get(ROUTE_PLANS_URL).param("repId", "2").with(repWithOwnId(1L)))
				.andExpect(status().isForbidden())
				.andExpect(content().contentType("application/problem+json"));
	}

	@Test
	void getAsRepWithoutRepIdParamDefaultsToOwn() throws Exception {
		when(routePlanService.plansFor(1L, null)).thenReturn(List.of());

		mockMvc.perform(get(ROUTE_PLANS_URL).with(repWithOwnId(1L)))
				.andExpect(status().isOk());

		verify(routePlanService).plansFor(1L, null);
	}

	@Test
	void getAsManagerWithoutRepIdIs400() throws Exception {
		mockMvc.perform(get(ROUTE_PLANS_URL).with(manager()))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType("application/problem+json"));
	}

	@Test
	void getByIdOfAnotherRepsPlanAsRepIsForbidden() throws Exception {
		when(routePlanService.byId(100L)).thenReturn(planFor(repWithId(2L), 100L));

		mockMvc.perform(get(ROUTE_PLANS_URL + "/100").with(repWithOwnId(1L)))
				.andExpect(status().isForbidden())
				.andExpect(content().contentType("application/problem+json"));
	}

	@Test
	void getByIdOfOwnPlanAsRepReturns200() throws Exception {
		when(routePlanService.byId(100L)).thenReturn(planFor(repWithId(1L), 100L));

		mockMvc.perform(get(ROUTE_PLANS_URL + "/100").with(repWithOwnId(1L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(100));
	}
}
