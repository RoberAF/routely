package io.github.roberaf.routely.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class CustomerTest {

	private static final org.locationtech.jts.geom.Point SOME_POINT = Points.of(43.0121, -7.5559);

	@Test
	void createRejectsOpenWindowWithoutClose() {
		assertThatIllegalArgumentException().isThrownBy(() -> Customer.create("Bar", "Rúa 1, Lugo", SOME_POINT,
				CustomerPriority.NORMAL, LocalTime.of(9, 0), null));
	}

	@Test
	void createRejectsCloseWindowWithoutOpen() {
		assertThatIllegalArgumentException().isThrownBy(() -> Customer.create("Bar", "Rúa 1, Lugo", SOME_POINT,
				CustomerPriority.NORMAL, null, LocalTime.of(13, 0)));
	}

	@Test
	void createRejectsOpenNotBeforeClose() {
		assertThatIllegalArgumentException().isThrownBy(() -> Customer.create("Bar", "Rúa 1, Lugo", SOME_POINT,
				CustomerPriority.NORMAL, LocalTime.of(13, 0), LocalTime.of(9, 0)));
	}

	@Test
	void createRejectsOpenEqualToClose() {
		assertThatIllegalArgumentException().isThrownBy(() -> Customer.create("Bar", "Rúa 1, Lugo", SOME_POINT,
				CustomerPriority.NORMAL, LocalTime.of(9, 0), LocalTime.of(9, 0)));
	}

	@Test
	void createRejectsBlankName() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> Customer.create("   ", "Rúa 1, Lugo", SOME_POINT, CustomerPriority.NORMAL, null, null));
	}

	@Test
	void createAcceptsValidWindow() {
		Customer customer = Customer.create("Bar", "Rúa 1, Lugo", SOME_POINT, CustomerPriority.KEY,
				LocalTime.of(9, 0), LocalTime.of(13, 30));

		assertThat(customer.getName()).isEqualTo("Bar");
		assertThat(customer.getTimeWindowOpen()).isEqualTo(LocalTime.of(9, 0));
		assertThat(customer.getTimeWindowClose()).isEqualTo(LocalTime.of(13, 30));
		assertThat(customer.isActive()).isTrue();
	}

	@Test
	void createAcceptsNoWindow() {
		Customer customer = Customer.create("Bar", "Rúa 1, Lugo", SOME_POINT, CustomerPriority.LOW, null, null);

		assertThat(customer.getTimeWindowOpen()).isNull();
		assertThat(customer.getTimeWindowClose()).isNull();
	}
}
