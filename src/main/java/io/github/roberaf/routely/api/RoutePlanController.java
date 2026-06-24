package io.github.roberaf.routely.api;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.roberaf.routely.api.dto.ComputeRouteRequest;
import io.github.roberaf.routely.api.dto.RoutePlanResponse;
import io.github.roberaf.routely.domain.RoutePlan;
import io.github.roberaf.routely.security.JwtService;
import io.github.roberaf.routely.service.PlanAccessDeniedException;
import io.github.roberaf.routely.service.RoutePlanService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/route-plans")
public class RoutePlanController {

	private static final String REP_ROLE = "REP";

	private final RoutePlanService routePlanService;

	public RoutePlanController(RoutePlanService routePlanService) {
		this.routePlanService = routePlanService;
	}

	@PostMapping("/compute")
	public ResponseEntity<RoutePlanResponse> compute(@Valid @RequestBody ComputeRouteRequest request) {
		RoutePlan plan = routePlanService.compute(request);
		return ResponseEntity.created(URI.create("/api/v1/route-plans/" + plan.getId()))
				.body(RoutePlanResponse.from(plan));
	}

	@GetMapping
	public List<RoutePlanResponse> list(
			@RequestParam Optional<Long> repId,
			@RequestParam Optional<LocalDate> date,
			@AuthenticationPrincipal Jwt jwt) {
		long effectiveRepId = resolveRepId(jwt, repId);
		return routePlanService.plansFor(effectiveRepId, date.orElse(null)).stream()
				.map(RoutePlanResponse::from)
				.toList();
	}

	@GetMapping("/{id}")
	public RoutePlanResponse byId(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
		RoutePlan plan = routePlanService.byId(id);
		if (isRep(jwt) && !Objects.equals(plan.getSalesRep().getId(), ownRepId(jwt))) {
			throw new PlanAccessDeniedException("plan " + id + " does not belong to this sales rep");
		}
		return RoutePlanResponse.from(plan);
	}

	private long resolveRepId(Jwt jwt, Optional<Long> requestedRepId) {
		if (isRep(jwt)) {
			long ownRepId = ownRepId(jwt);
			if (requestedRepId.isPresent() && requestedRepId.get() != ownRepId) {
				throw new PlanAccessDeniedException("cannot read another sales rep's plans");
			}
			return ownRepId;
		}
		return requestedRepId.orElseThrow(() -> new IllegalArgumentException("repId is required"));
	}

	private boolean isRep(Jwt jwt) {
		return REP_ROLE.equals(jwt.getClaimAsString(JwtService.CLAIM_ROLE));
	}

	private long ownRepId(Jwt jwt) {
		Number repId = jwt.getClaim(JwtService.CLAIM_REP_ID);
		return repId.longValue();
	}
}
