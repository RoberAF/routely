package io.github.roberaf.routely.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.roberaf.routely.domain.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

	List<Customer> findByActiveTrue();

	@Query(value = """
			SELECT * FROM customer c
			WHERE c.active = true
			  AND ST_DWithin(geography(c.location), geography(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)), :radiusMeters)
			ORDER BY ST_Distance(geography(c.location), geography(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)))
			""", nativeQuery = true)
	List<Customer> findNearby(@Param("lat") double lat, @Param("lng") double lng,
			@Param("radiusMeters") double radiusMeters);
}
