package rcs;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

public class RcsLineChartPanel extends JPanel {
    private List<Double> freqs = new ArrayList<>();
    private List<Double> rcsValues = new ArrayList<>();

    public void updateData(List<Double> freqsGHz, List<Double> rcsDbsm) {
        this.freqs = new ArrayList<>(freqsGHz);
        this.rcsValues = new ArrayList<>(rcsDbsm);
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

        if (freqs.isEmpty() || rcsValues.isEmpty()) {
            return;
        }

        double minFreq = freqs.stream().min(Double::compareTo).orElse(0.0);
        double maxFreq = freqs.stream().max(Double::compareTo).orElse(1.0);
        double minRcs = rcsValues.stream().min(Double::compareTo).orElse(0.0);
        double maxRcs = rcsValues.stream().max(Double::compareTo).orElse(1.0);
        if (minRcs == maxRcs) {
            maxRcs = minRcs + 1.0;
        }

        FontMetrics fm = g2.getFontMetrics();
        int tickCount = 4;

        g2.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i <= tickCount; i++) {
            int y = margin + i * plotHeight / tickCount;
            g2.drawLine(margin, y, margin + plotWidth, y);
        }

        g2.setColor(Color.GRAY);
        for (int i = 0; i <= tickCount; i++) {
            double rcsTick = minRcs + (maxRcs - minRcs) * i / tickCount;
            int y = margin + plotHeight - (int) ((rcsTick - minRcs) / (maxRcs - minRcs) * plotHeight);
            g2.drawLine(margin - 4, y, margin, y);
            String label = formatTick(rcsTick);
            g2.drawString(label, margin - 8 - fm.stringWidth(label), y + fm.getAscent() / 2 - 2);
        }

        for (int i = 0; i <= tickCount; i++) {
            double freqTick = minFreq + (maxFreq - minFreq) * i / tickCount;
            int x = margin + (int) ((freqTick - minFreq) / (maxFreq - minFreq) * plotWidth);
            g2.drawLine(x, margin + plotHeight, x, margin + plotHeight + 4);
            String label = formatTick(freqTick);
            g2.drawString(label, x - fm.stringWidth(label) / 2, margin + plotHeight + fm.getAscent() + 6);
        }

        g2.setColor(Color.BLUE);
        g2.setStroke(new BasicStroke(2f));
        int prevX = -1;
        int prevY = -1;
        for (int i = 0; i < freqs.size(); i++) {
            double f = freqs.get(i);
            double rcs = rcsValues.get(i);
            int x = margin + (int) ((f - minFreq) / (maxFreq - minFreq) * plotWidth);
            int y = margin + plotHeight - (int) ((rcs - minRcs) / (maxRcs - minRcs) * plotHeight);
            if (prevX >= 0) {
                g2.drawLine(prevX, prevY, x, y);
            }
            g2.fillOval(x - 3, y - 3, 6, 6);
            prevX = x;
            prevY = y;
        }

        g2.setColor(Color.DARK_GRAY);
        String xLabel = "频率 (GHz)";
        String yLabel = "RCS (dBsm)";
        g2.drawString(xLabel, width / 2 - fm.stringWidth(xLabel) / 2, height - 8);
        g2.rotate(-Math.PI / 2);
        g2.drawString(yLabel, -height / 2 - fm.stringWidth(yLabel) / 2, 16);
        g2.rotate(Math.PI / 2);
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
}
