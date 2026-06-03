package io.github.roberaf.routely.domain;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * Factory for JTS {@link Point} instances using the WGS84 (SRID 4326) spatial
 * reference system. This is the only place in the codebase that should build a
 * {@link Point} directly, so the latitude/longitude ordering stays consistent.
 */
public final class Points {

	private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

	private Points() {
	}

	/**
	 * Creates a WGS84 point. Note that JTS coordinates are (x, y), i.e.
	 * (longitude, latitude) - the opposite order of the method parameters here.
	 */
	public static Point of(double lat, double lng) {
		return FACTORY.createPoint(new Coordinate(lng, lat));
	}
}
