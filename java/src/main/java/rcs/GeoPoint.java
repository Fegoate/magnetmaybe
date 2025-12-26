package rcs;

public record GeoPoint(double latitudeDeg, double longitudeDeg, double altitudeM) {
    private static final double WGS84_A = 6378137.0;
    private static final double WGS84_E2 = 6.69437999014e-3;

    public Vector3 toEcef() {
        double lat = Math.toRadians(latitudeDeg);
        double lon = Math.toRadians(longitudeDeg);
        double sinLat = Math.sin(lat);
        double cosLat = Math.cos(lat);
        double sinLon = Math.sin(lon);
        double cosLon = Math.cos(lon);

        double n = WGS84_A / Math.sqrt(1.0 - WGS84_E2 * sinLat * sinLat);
        double x = (n + altitudeM) * cosLat * cosLon;
        double y = (n + altitudeM) * cosLat * sinLon;
        double z = (n * (1.0 - WGS84_E2) + altitudeM) * sinLat;
        return new Vector3(x, y, z);
    }
}
