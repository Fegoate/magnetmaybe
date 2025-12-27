package rcs;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class DirectionHeatmapPanel extends JPanel {
    private double[] azimuthsDeg = new double[0];
    private double[] elevationsDeg = new double[0];
    private double[][] rcsValues = new double[0][0];
    private Double incidenceAz;
    private Double incidenceEl;
    private Double scatterAz;
    private Double scatterEl;

    public void updateData(double[] azDeg, double[] elDeg, double[][] rcsGrid,
                           double incidenceAzDeg, double incidenceElDeg,
                           double scatterAzDeg, double scatterElDeg) {
        this.azimuthsDeg = azDeg;
        this.elevationsDeg = elDeg;
        this.rcsValues = rcsGrid;
        this.incidenceAz = incidenceAzDeg;
        this.incidenceEl = incidenceElDeg;
        this.scatterAz = scatterAzDeg;
        this.scatterEl = scatterElDeg;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = getWidth();
        int height = getHeight();
        int margin = 40;
        int plotWidth = width - 2 * margin;
        int plotHeight = height - 2 * margin;

        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);
        g2.setColor(Color.GRAY);
        g2.drawRect(margin, margin, plotWidth, plotHeight);

        if (azimuthsDeg.length == 0 || elevationsDeg.length == 0
                || rcsValues.length == 0 || rcsValues[0].length == 0) {
            return;
        }

        double azMin = azimuthsDeg[0];
        double azMax = azimuthsDeg[azimuthsDeg.length - 1];
        double elMin = elevationsDeg[0];
        double elMax = elevationsDeg[elevationsDeg.length - 1];
        if (azMax == azMin) {
            azMax = azMin + 1.0;
        }
        if (elMax == elMin) {
            elMax = elMin + 1.0;
        }

        double minRcs = Double.POSITIVE_INFINITY;
        double maxRcs = Double.NEGATIVE_INFINITY;
        int rows = Math.min(elevationsDeg.length, rcsValues.length);
        int cols = Math.min(azimuthsDeg.length, rcsValues[0].length);
        for (int i = 0; i < rows; i++) {
            double[] row = rcsValues[i];
            int limit = Math.min(cols, row.length);
            for (int j = 0; j < limit; j++) {
                double v = row[j];
                minRcs = Math.min(minRcs, v);
                maxRcs = Math.max(maxRcs, v);
            }
        }
        double scale = Math.max(1e-6, maxRcs - minRcs);

        double[] azBounds = computeBounds(azimuthsDeg);
        double[] elBounds = computeBounds(elevationsDeg);
        for (int i = 0; i < rows; i++) {
            double[] row = rcsValues[i];
            int limit = Math.min(cols, row.length);
            for (int j = 0; j < limit; j++) {
                double v = row[j];
                float t = (float) ((v - minRcs) / scale);
                Color c = new Color(t, 0.2f, 1.0f - t);
                g2.setColor(c);

                int x0 = valueToX(azBounds[j], azMin, azMax, margin, plotWidth);
                int x1 = valueToX(azBounds[j + 1], azMin, azMax, margin, plotWidth);
                int y0 = valueToY(elBounds[i], elMin, elMax, margin, plotHeight);
                int y1 = valueToY(elBounds[i + 1], elMin, elMax, margin, plotHeight);
                int x = Math.min(x0, x1);
                int y = Math.min(y0, y1);
                int cellW = Math.max(1, Math.abs(x1 - x0));
                int cellH = Math.max(1, Math.abs(y1 - y0));
                g2.fillRect(x, y, cellW, cellH);
            }
        }

        g2.setColor(Color.BLACK);
        FontMetrics fm = g2.getFontMetrics();
        int tickCount = 4;

        for (int i = 0; i <= tickCount; i++) {
            double azTick = azMin + (azMax - azMin) * i / tickCount;
            int x = valueToX(azTick, azMin, azMax, margin, plotWidth);
            g2.drawLine(x, margin + plotHeight, x, margin + plotHeight + 4);
            String label = formatTick(azTick);
            g2.drawString(label, x - fm.stringWidth(label) / 2, margin + plotHeight + fm.getAscent() + 6);
        }

        for (int i = 0; i <= tickCount; i++) {
            double elTick = elMin + (elMax - elMin) * i / tickCount;
            int y = valueToY(elTick, elMin, elMax, margin, plotHeight);
            g2.drawLine(margin - 4, y, margin, y);
            String label = formatTick(elTick);
            g2.drawString(label, margin - 8 - fm.stringWidth(label), y + fm.getAscent() / 2 - 2);
        }

        String xLabel = "散射方位角 (deg)";
        String yLabel = "散射俯仰角 (deg)";
        g2.drawString(xLabel, width / 2 - fm.stringWidth(xLabel) / 2, height - 8);
        g2.rotate(-Math.PI / 2);
        g2.drawString(yLabel, -height / 2 - fm.stringWidth(yLabel) / 2, 16);
        g2.rotate(Math.PI / 2);

        g2.setColor(Color.GREEN.darker());
        drawMarker(g2, incidenceAz, incidenceEl, margin, plotWidth, plotHeight,
                azMin, azMax, elMin, elMax);
        g2.setColor(Color.RED.darker());
        drawMarker(g2, scatterAz, scatterEl, margin, plotWidth, plotHeight,
                azMin, azMax, elMin, elMax);
    }

    private String formatTick(double value) {
        double abs = Math.abs(value);
        if (abs >= 100) {
            return String.format("%.0f", value);
        } else if (abs >= 10) {
            return String.format("%.1f", value);
        }
        return String.format("%.2f", value);
    }

    private void drawMarker(Graphics2D g2, Double azDeg, Double elDeg, int margin,
                            int plotWidth, int plotHeight,
                            double azMin, double azMax, double elMin, double elMax) {
        if (azDeg == null || elDeg == null) {
            return;
        }
        int x = valueToX(azDeg, azMin, azMax, margin, plotWidth);
        int y = valueToY(elDeg, elMin, elMax, margin, plotHeight);
        g2.drawOval(x - 5, y - 5, 10, 10);
        g2.drawLine(x - 8, y, x + 8, y);
        g2.drawLine(x, y - 8, x, y + 8);
    }

    private int valueToX(double value, double min, double max, int margin, int plotWidth) {
        return margin + (int) Math.round((value - min) / (max - min) * plotWidth);
    }

    private int valueToY(double value, double min, double max, int margin, int plotHeight) {
        return margin + plotHeight - (int) Math.round((value - min) / (max - min) * plotHeight);
    }

    private double[] computeBounds(double[] coordinates) {
        double[] bounds = new double[coordinates.length + 1];
        if (coordinates.length == 1) {
            bounds[0] = coordinates[0] - 0.5;
            bounds[1] = coordinates[0] + 0.5;
            return bounds;
        }

        bounds[0] = coordinates[0] - (coordinates[1] - coordinates[0]) / 2.0;
        for (int i = 1; i < coordinates.length; i++) {
            bounds[i] = (coordinates[i - 1] + coordinates[i]) / 2.0;
        }
        bounds[coordinates.length] = coordinates[coordinates.length - 1]
                + (coordinates[coordinates.length - 1] - coordinates[coordinates.length - 2]) / 2.0;
        return bounds;
    }
}
