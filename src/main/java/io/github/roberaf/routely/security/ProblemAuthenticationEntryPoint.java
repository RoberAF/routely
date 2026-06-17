package io.github.roberaf.routely.security;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes an RFC 7807 body for requests that reach a protected endpoint
 * without a valid bearer token, instead of Spring Security's default
 * plain-text 401 page.
 */
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private static final String PROBLEM_JSON = "application/problem+json";

	private final ObjectMapper objectMapper;

	public ProblemAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(PROBLEM_JSON);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("type", "about:blank");
		body.put("title", "Unauthorized");
		body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
		body.put("detail", "Full authentication is required");

		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
