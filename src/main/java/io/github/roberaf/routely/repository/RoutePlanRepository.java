package io.github.roberaf.routely.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.roberaf.routely.domain.RoutePlan;

public interface RoutePlanRepository extends JpaRepository<RoutePlan, Long> {

	List<RoutePlan> findBySalesRepId(Long repId);

	List<RoutePlan> findBySalesRepIdAndPlanDate(Long repId, LocalDate planDate);
}
