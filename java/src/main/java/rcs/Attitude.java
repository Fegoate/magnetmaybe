package rcs;

public record Attitude(double yawDeg, double pitchDeg, double rollDeg) {
    public double[][] bodyToGeocentricMatrix() {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double roll = Math.toRadians(rollDeg);

        double cy = Math.cos(yaw);
        double sy = Math.sin(yaw);
        double cp = Math.cos(pitch);
        double sp = Math.sin(pitch);
        double cr = Math.cos(roll);
        double sr = Math.sin(roll);

        double[][] rz = {
            {cy, -sy, 0.0},
            {sy, cy, 0.0},
            {0.0, 0.0, 1.0}
        };
        double[][] ry = {
            {cp, 0.0, sp},
            {0.0, 1.0, 0.0},
            {-sp, 0.0, cp}
        };
        double[][] rx = {
            {1.0, 0.0, 0.0},
            {0.0, cr, -sr},
            {0.0, sr, cr}
        };

        return multiply(multiply(rz, ry), rx);
    }

    public double[][] geocentricToBodyMatrix() {
        return transpose(bodyToGeocentricMatrix());
    }

    public Vector3 geocentricToBody(Vector3 v) {
        double[][] r = geocentricToBodyMatrix();
        double bx = r[0][0] * v.x() + r[0][1] * v.y() + r[0][2] * v.z();
        double by = r[1][0] * v.x() + r[1][1] * v.y() + r[1][2] * v.z();
        double bz = r[2][0] * v.x() + r[2][1] * v.y() + r[2][2] * v.z();
        return new Vector3(bx, by, bz);
    }

    private double[][] multiply(double[][] a, double[][] b) {
        double[][] c = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                c[i][j] = a[i][0] * b[0][j] + a[i][1] * b[1][j] + a[i][2] * b[2][j];
            }
        }
        return c;
    }

    private double[][] transpose(double[][] a) {
        double[][] t = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                t[i][j] = a[j][i];
            }
        }
        return t;
    }
}
