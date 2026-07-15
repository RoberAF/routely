package io.github.roberaf.routely.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

class PointsTest {

	@Test
	void ofBuildsWgs84PointWithLatLngOrdering() {
		Point point = Points.of(43.0, -7.5);

		assertThat(point.getY()).isEqualTo(43.0);
		assertThat(point.getX()).isEqualTo(-7.5);
		assertThat(point.getSRID()).isEqualTo(4326);
	}
}
