package rcs;

public class SyntheticRcsModel {
    public double bistaticRcs(double freqGHz, double incidenceAzDeg, double incidenceElDeg,
                              double scatterAzDeg, double scatterElDeg) {
        double base = 10.0 + 5.0 * Math.sin(Math.toRadians(freqGHz * 12.0));
        double incidenceTerm = angularGain(incidenceAzDeg, incidenceElDeg, 35.0, 12.0);
        double scatterTerm = angularGain(scatterAzDeg, scatterElDeg, -20.0, 10.0);
        double crossCoupling = 3.0 * Math.cos(Math.toRadians(incidenceAzDeg - scatterAzDeg))
                * Math.cos(Math.toRadians(incidenceElDeg - scatterElDeg));
        return base + incidenceTerm + scatterTerm + crossCoupling;
    }

    public double[][] directionGrid(double freqGHz, double[] azimuthsDeg, double[] elevationsDeg,
                                     double refIncidenceAz, double refIncidenceEl,
                                     double refScatterAz, double refScatterEl) {
        double[][] grid = new double[elevationsDeg.length][azimuthsDeg.length];
        for (int i = 0; i < elevationsDeg.length; i++) {
            for (int j = 0; j < azimuthsDeg.length; j++) {
                grid[i][j] = bistaticRcs(freqGHz, refIncidenceAz, refIncidenceEl,
                        azimuthsDeg[j], elevationsDeg[i]);
            }
        }
        return grid;
    }

    private double angularGain(double azDeg, double elDeg, double preferredAzDeg, double widthDeg) {
        double azDelta = normalizeAngleDeg(azDeg - preferredAzDeg);
        double elDelta = normalizeAngleDeg(elDeg);
        double azFactor = Math.exp(-Math.pow(azDelta / widthDeg, 2));
        double elFactor = Math.exp(-Math.pow(elDelta / widthDeg, 2));
        return 12.0 * azFactor * elFactor;
    }

    private double normalizeAngleDeg(double angle) {
        double a = angle % 360.0;
        if (a > 180.0) {
            return a - 360.0;
        }
        if (a < -180.0) {
            return a + 360.0;
        }
        return a;
    }
}
