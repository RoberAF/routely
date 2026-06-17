package io.github.roberaf.routely.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.roberaf.routely.domain.AppUser;
import io.github.roberaf.routely.repository.AppUserRepository;
import jakarta.validation.Valid;

@RestController
public class AuthController {

	private final AppUserRepository appUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthController(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
			JwtService jwtService) {
		this.appUserRepository = appUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@PostMapping("/api/v1/auth/login")
	public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		AppUser user = appUserRepository.findByEmail(request.email())
				.orElseThrow(InvalidCredentialsException::new);

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}

		String accessToken = jwtService.issueToken(user);
		TokenResponse body = new TokenResponse(accessToken, "Bearer", jwtService.expiresInSeconds());
		return ResponseEntity.status(HttpStatus.OK).body(body);
	}
}
