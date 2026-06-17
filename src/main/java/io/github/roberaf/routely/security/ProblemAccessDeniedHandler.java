package io.github.roberaf.routely.security;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes an RFC 7807 body for authenticated callers whose role doesn't
 * satisfy an endpoint's authorization rule.
 */
public class ProblemAccessDeniedHandler implements AccessDeniedHandler {

	private static final String PROBLEM_JSON = "application/problem+json";

	private final ObjectMapper objectMapper;

	public ProblemAccessDeniedHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType(PROBLEM_JSON);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("type", "about:blank");
		body.put("title", "Forbidden");
		body.put("status", HttpServletResponse.SC_FORBIDDEN);
		body.put("detail", "Insufficient permissions");

		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
