package io.github.roberaf.routely.optimizer;

/**
 * Great-circle distance between two {@link GeoPoint}s using the haversine formula.
 * This is an approximation - it treats the earth as a perfect sphere, so error
 * grows for very long distances, but it is more than accurate enough for the
 * regional (single sales territory) distances this optimizer deals with.
 */
public final class Haversine {

	public static final double EARTH_RADIUS_METERS = 6_371_000.0;

	private Haversine() {
	}

	public static double distanceMeters(GeoPoint a, GeoPoint b) {
		double lat1 = Math.toRadians(a.lat());
		double lat2 = Math.toRadians(b.lat());
		double dLat = Math.toRadians(b.lat() - a.lat());
		double dLng = Math.toRadians(b.lng() - a.lng());

		double sinHalfLat = Math.sin(dLat / 2);
		double sinHalfLng = Math.sin(dLng / 2);
		double h = sinHalfLat * sinHalfLat + Math.cos(lat1) * Math.cos(lat2) * sinHalfLng * sinHalfLng;
		double c = 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
		return EARTH_RADIUS_METERS * c;
	}
}
