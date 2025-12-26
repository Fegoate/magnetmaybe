package rcs;

public record Vector3(double x, double y, double z) {
    public Vector3 add(Vector3 other) {
        return new Vector3(x + other.x, y + other.y, z + other.z);
    }

    public Vector3 subtract(Vector3 other) {
        return new Vector3(x - other.x, y - other.y, z - other.z);
    }

    public Vector3 scale(double factor) {
        return new Vector3(x * factor, y * factor, z * factor);
    }

    public double dot(Vector3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public double norm() {
        return Math.sqrt(dot(this));
    }

    public Vector3 normalize() {
        double n = norm();
        if (n == 0.0) {
            return this;
        }
        return scale(1.0 / n);
    }
}
