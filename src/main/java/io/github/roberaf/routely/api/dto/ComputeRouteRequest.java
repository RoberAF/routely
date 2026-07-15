package io.github.roberaf.routely.api.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;

/**
 * {@code customerIds} is optional: leave it null (or empty) to route every
 * active customer.
 */
public record ComputeRouteRequest(@NotNull Long repId, @NotNull LocalDate planDate, List<Long> customerIds) {
}
