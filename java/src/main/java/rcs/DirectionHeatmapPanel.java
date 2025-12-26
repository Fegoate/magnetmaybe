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

        if (azimuthsDeg.length == 0 || elevationsDeg.length == 0) {
            return;
        }

        double minRcs = Double.POSITIVE_INFINITY;
        double maxRcs = Double.NEGATIVE_INFINITY;
        for (double[] row : rcsValues) {
            for (double v : row) {
                minRcs = Math.min(minRcs, v);
                maxRcs = Math.max(maxRcs, v);
            }
        }
        double scale = Math.max(1e-6, maxRcs - minRcs);

        int cellW = Math.max(1, plotWidth / Math.max(1, azimuthsDeg.length - 1));
        int cellH = Math.max(1, plotHeight / Math.max(1, elevationsDeg.length - 1));
        for (int i = 0; i < elevationsDeg.length; i++) {
            for (int j = 0; j < azimuthsDeg.length; j++) {
                double v = rcsValues[i][j];
                float t = (float) ((v - minRcs) / scale);
                Color c = new Color(t, 0.2f, 1.0f - t);
                g2.setColor(c);
                int x = margin + j * cellW;
                int y = margin + (elevationsDeg.length - 1 - i) * cellH;
                g2.fillRect(x, y, cellW, cellH);
            }
        }

        g2.setColor(Color.BLACK);
        FontMetrics fm = g2.getFontMetrics();
        String xLabel = "散射方位角 (deg)";
        String yLabel = "散射俯仰角 (deg)";
        g2.drawString(xLabel, width / 2 - fm.stringWidth(xLabel) / 2, height - 8);
        g2.rotate(-Math.PI / 2);
        g2.drawString(yLabel, -height / 2 - fm.stringWidth(yLabel) / 2, 16);
        g2.rotate(Math.PI / 2);

        g2.setColor(Color.GREEN.darker());
        drawMarker(g2, incidenceAz, incidenceEl, margin, plotWidth, plotHeight);
        g2.setColor(Color.RED.darker());
        drawMarker(g2, scatterAz, scatterEl, margin, plotWidth, plotHeight);
    }

    private void drawMarker(Graphics2D g2, Double azDeg, Double elDeg, int margin, int plotWidth, int plotHeight) {
        if (azDeg == null || elDeg == null) {
            return;
        }
        double azMin = azimuthsDeg[0];
        double azMax = azimuthsDeg[azimuthsDeg.length - 1];
        double elMin = elevationsDeg[0];
        double elMax = elevationsDeg[elevationsDeg.length - 1];

        int x = margin + (int) ((azDeg - azMin) / (azMax - azMin) * plotWidth);
        int y = margin + plotHeight - (int) ((elDeg - elMin) / (elMax - elMin) * plotHeight);
        g2.drawOval(x - 5, y - 5, 10, 10);
        g2.drawLine(x - 8, y, x + 8, y);
        g2.drawLine(x, y - 8, x, y + 8);
    }
}
