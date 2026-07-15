package io.github.roberaf.routely.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class RoutePlanTest {

	@Test
	void addStopAssignsSequenceIndexInOrder() {
		SalesRep rep = new SalesRep("Laura Castro", "laura.castro@routely.dev", Points.of(43.0121, -7.5559));
		RoutePlan plan = new RoutePlan(rep, LocalDate.of(2026, 7, 15));

		Customer first = Customer.create("Cafetería Rúa Nova", "Rúa Nova 12, Lugo", Points.of(43.0096, -7.5560),
				CustomerPriority.NORMAL, null, null);
		Customer second = Customer.create("Estanco Porta Miñá", "Porta Miñá 3, Lugo", Points.of(43.0080, -7.5620),
				CustomerPriority.NORMAL, LocalTime.of(9, 30), LocalTime.of(13, 30));
		Customer third = Customer.create("Ferretería As Termas", "Rúa das Termas 5, Lugo", Points.of(43.0205, -7.5490),
				CustomerPriority.KEY, null, null);

		RouteStop stop1 = plan.addStop(first, LocalTime.of(9, 0));
		RouteStop stop2 = plan.addStop(second, LocalTime.of(9, 30));
		RouteStop stop3 = plan.addStop(third, null);

		assertThat(plan.getStops()).containsExactly(stop1, stop2, stop3);
		assertThat(stop1.getSequenceIndex()).isZero();
		assertThat(stop2.getSequenceIndex()).isEqualTo(1);
		assertThat(stop3.getSequenceIndex()).isEqualTo(2);
		assertThat(stop3.getEstimatedArrival()).isNull();
		assertThat(stop1.getRoutePlan()).isSameAs(plan);
		assertThat(stop1.getCustomer()).isSameAs(first);
	}
}
