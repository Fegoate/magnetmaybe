package rcs;

import java.util.ArrayList;
import java.util.List;

public class RcsCalculator {
    private final SyntheticRcsModel model;

    public record BistaticGeometry(Vector3 incidenceDirBody, Vector3 scatterDirBody,
                                   double incidenceAzDeg, double incidenceElDeg,
                                   double scatterAzDeg, double scatterElDeg) { }

    public record SweepResult(List<Double> freqsGHz, List<Double> rcsValues) { }

    public RcsCalculator(SyntheticRcsModel model) {
        this.model = model;
    }

    public BistaticGeometry geometry(GeoPoint transmitter, GeoPoint receiver, GeoPoint target, Attitude attitude) {
        Vector3 txEcef = transmitter.toEcef();
        Vector3 rxEcef = receiver.toEcef();
        Vector3 tgtEcef = target.toEcef();

        Vector3 incidenceGeo = txEcef.subtract(tgtEcef).normalize();
        Vector3 scatterGeo = rxEcef.subtract(tgtEcef).normalize();

        Vector3 incidenceBody = attitude.geocentricToBody(incidenceGeo);
        Vector3 scatterBody = attitude.geocentricToBody(scatterGeo);

        double incidenceAz = Math.toDegrees(Math.atan2(incidenceBody.y(), incidenceBody.x()));
        double incidenceEl = Math.toDegrees(Math.asin(incidenceBody.z() / incidenceBody.norm()));
        double scatterAz = Math.toDegrees(Math.atan2(scatterBody.y(), scatterBody.x()));
        double scatterEl = Math.toDegrees(Math.asin(scatterBody.z() / scatterBody.norm()));

        return new BistaticGeometry(incidenceBody, scatterBody, incidenceAz, incidenceEl, scatterAz, scatterEl);
    }

    public SweepResult sweep(double startGHz, double stopGHz, int points,
                             double incidenceAzDeg, double incidenceElDeg,
                             double scatterAzDeg, double scatterElDeg) {
        List<Double> freqs = new ArrayList<>();
        List<Double> rcsValues = new ArrayList<>();
        if (points < 2) {
            points = 2;
        }
        double step = (stopGHz - startGHz) / (points - 1);
        for (int i = 0; i < points; i++) {
            double freq = startGHz + i * step;
            freqs.add(freq);
            rcsValues.add(model.bistaticRcs(freq, incidenceAzDeg, incidenceElDeg, scatterAzDeg, scatterElDeg));
        }
        return new SweepResult(freqs, rcsValues);
    }

    public double[][] directionGrid(double freqGHz, double[] azDeg, double[] elDeg,
                                     double incidenceAzDeg, double incidenceElDeg,
                                     double scatterAzDeg, double scatterElDeg) {
        return model.directionGrid(freqGHz, azDeg, elDeg, incidenceAzDeg, incidenceElDeg, scatterAzDeg, scatterElDeg);
    }
}
