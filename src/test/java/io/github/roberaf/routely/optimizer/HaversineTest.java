package io.github.roberaf.routely.optimizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class HaversineTest {

	@Test
	void distanceFromPointToItselfIsZero() {
		GeoPoint p = new GeoPoint(43.0121, -7.5559);

		assertThat(Haversine.distanceMeters(p, p)).isZero();
	}

	@Test
	void distanceIsSymmetric() {
		GeoPoint a = new GeoPoint(43.0121, -7.5559);
		GeoPoint b = new GeoPoint(40.4168, -3.7038);

		assertThat(Haversine.distanceMeters(a, b)).isEqualTo(Haversine.distanceMeters(b, a));
	}

	@Test
	void oneDegreeOfLongitudeAtTheEquator() {
		GeoPoint a = new GeoPoint(0, 0);
		GeoPoint b = new GeoPoint(0, 1);

		assertThat(Haversine.distanceMeters(a, b)).isCloseTo(111_194.93, within(1.0));
	}

	@Test
	void antipodalPointsAreHalfTheEarthsCircumferenceApart() {
		GeoPoint a = new GeoPoint(0, 0);
		GeoPoint b = new GeoPoint(0, 180);

		assertThat(Haversine.distanceMeters(a, b)).isCloseTo(20_015_086.8, within(100.0));
	}

	@Test
	void madridToBarcelona() {
		GeoPoint madrid = new GeoPoint(40.4168, -3.7038);
		GeoPoint barcelona = new GeoPoint(41.3874, 2.1686);

		assertThat(Haversine.distanceMeters(madrid, barcelona)).isCloseTo(505_300.0, within(5_000.0));
	}

	@Test
	void geoPointRejectsLatitudeOutOfRange() {
		assertThatIllegalArgumentException().isThrownBy(() -> new GeoPoint(91, 0));
	}

	@Test
	void geoPointRejectsLongitudeOutOfRange() {
		assertThatIllegalArgumentException().isThrownBy(() -> new GeoPoint(0, -181));
	}
}
