package io.github.roberaf.routely.api.dto;

import java.time.LocalTime;

import io.github.roberaf.routely.domain.CustomerPriority;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CustomerCreateRequest(
		@NotBlank String name,
		@NotBlank String address,
		@NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
		@NotNull @DecimalMin("-180") @DecimalMax("180") Double lng,
		@NotNull CustomerPriority priority,
		LocalTime timeWindowOpen,
		LocalTime timeWindowClose) {
}
