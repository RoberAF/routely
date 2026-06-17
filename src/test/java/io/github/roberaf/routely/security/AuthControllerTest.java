package io.github.roberaf.routely.security;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import io.github.roberaf.routely.domain.AppUser;
import io.github.roberaf.routely.domain.Role;
import io.github.roberaf.routely.repository.AppUserRepository;

@WebMvcTest(controllers = AuthController.class)
@Import({ SecurityConfig.class, JwtService.class, AuthExceptionHandler.class })
class AuthControllerTest {

	private static final String LOGIN_URL = "/api/v1/auth/login";
	private static final String KNOWN_PASSWORD = "secret123";
	private static final String KNOWN_HASH = new BCryptPasswordEncoder().encode(KNOWN_PASSWORD);

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AppUserRepository appUserRepository;

	private static AppUser userWithId(long id, String email, Role role) {
		AppUser user = new AppUser(email, KNOWN_HASH, role, null);
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	@Test
	void correctCredentialsReturnAccessTokenAndBearerType() throws Exception {
		AppUser user = userWithId(9L, "manager@routely.dev", Role.MANAGER);
		when(appUserRepository.findByEmail("manager@routely.dev")).thenReturn(Optional.of(user));

		mockMvc.perform(post(LOGIN_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"manager@routely.dev","password":"secret123"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value(Matchers.not(Matchers.blankOrNullString())))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresInSeconds").value(28800));
	}

	@Test
	void wrongPasswordIsRejectedAsUnauthorizedProblemJson() throws Exception {
		AppUser user = userWithId(9L, "manager@routely.dev", Role.MANAGER);
		when(appUserRepository.findByEmail("manager@routely.dev")).thenReturn(Optional.of(user));

		mockMvc.perform(post(LOGIN_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"manager@routely.dev","password":"wrong-password"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType("application/problem+json"))
				.andExpect(jsonPath("$.title").value("Unauthorized"))
				.andExpect(jsonPath("$.detail").value("Invalid email or password"));
	}

	@Test
	void unknownEmailIsRejectedWithTheSameBodyShapeAsWrongPassword() throws Exception {
		when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());

		mockMvc.perform(post(LOGIN_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"nobody@routely.dev","password":"whatever123"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType("application/problem+json"))
				.andExpect(jsonPath("$.title").value("Unauthorized"))
				.andExpect(jsonPath("$.detail").value("Invalid email or password"));
	}

	@Test
	void missingEmailFailsBeanValidationWith400() throws Exception {
		mockMvc.perform(post(LOGIN_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"password":"whatever123"}
								"""))
				.andExpect(status().isBadRequest());
	}
}
