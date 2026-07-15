package io.github.roberaf.routely.security;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import tools.jackson.databind.ObjectMapper;

/**
 * Stateless resource-server setup: every request is authorized off a
 * self-issued HS256 JWT, there is no session and no CSRF token to manage.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

	private final JwtProperties jwtProperties;

	public SecurityConfig(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	private SecretKeySpec secretKey() {
		byte[] secretBytes = jwtProperties.jwtSecret().getBytes(StandardCharsets.UTF_8);
		return new SecretKeySpec(secretBytes, "HmacSHA256");
	}

	@Bean
	public JwtEncoder jwtEncoder() {
		return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
	}

	@Bean
	public JwtDecoder jwtDecoder() {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey())
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer("routely");
		decoder.setJwtValidator(validator);
		return decoder;
	}

	private JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		Converter<Jwt, Collection<GrantedAuthority>> authoritiesConverter = jwt -> {
			String role = jwt.getClaimAsString(JwtService.CLAIM_ROLE);
			return role == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role));
		};
		converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return converter;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
		ProblemAuthenticationEntryPoint entryPoint = new ProblemAuthenticationEntryPoint(objectMapper);
		ProblemAccessDeniedHandler accessDeniedHandler = new ProblemAccessDeniedHandler(objectMapper);

		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
						.requestMatchers(HttpMethod.GET, "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
						.permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/customers").hasAnyRole("ADMIN", "MANAGER")
						.requestMatchers(HttpMethod.POST, "/api/v1/route-plans/compute")
						.hasAnyRole("ADMIN", "MANAGER")
						.requestMatchers(HttpMethod.GET, "/api/v1/customers/**").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/v1/route-plans/**").authenticated()
						.anyRequest().authenticated())
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint(entryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
						.authenticationEntryPoint(entryPoint)
						.accessDeniedHandler(accessDeniedHandler));

		return http.build();
	}
}
