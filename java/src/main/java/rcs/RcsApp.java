package rcs;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.stream.DoubleStream;

public class RcsApp extends JFrame {
    private final JTextField freqStartField = new JTextField("8.0");
    private final JTextField freqStopField = new JTextField("12.0");
    private final JTextField freqCountField = new JTextField("40");

    private final JTextField yawField = new JTextField("0");
    private final JTextField pitchField = new JTextField("0");
    private final JTextField rollField = new JTextField("0");

    private final JTextField txLatField = new JTextField("30.0");
    private final JTextField txLonField = new JTextField("-100.0");
    private final JTextField txAltField = new JTextField("100.0");

    private final JTextField rxLatField = new JTextField("25.0");
    private final JTextField rxLonField = new JTextField("-90.0");
    private final JTextField rxAltField = new JTextField("100.0");

    private final JTextField tgtLatField = new JTextField("28.0");
    private final JTextField tgtLonField = new JTextField("-95.0");
    private final JTextField tgtAltField = new JTextField("10000.0");

    private final JLabel geometryLabel = new JLabel("入射/散射方向：-");

    private final RcsLineChartPanel lineChart = new RcsLineChartPanel();
    private final DirectionHeatmapPanel heatmap = new DirectionHeatmapPanel();

    private final RcsCalculator calculator = new RcsCalculator(new SyntheticRcsModel());

    public RcsApp() {
        super("双站 RCS 插值（Java 演示）");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1200, 700));

        JPanel inputPanel = buildInputPanel();
        JPanel plots = new JPanel(new GridLayout(1, 2, 10, 10));
        plots.add(lineChart);
        plots.add(heatmap);
        plots.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(inputPanel, BorderLayout.WEST);
        add(plots, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildInputPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(sectionLabel("频率设置"));
        panel.add(labeledField("起始 (GHz)", freqStartField));
        panel.add(labeledField("终止 (GHz)", freqStopField));
        panel.add(labeledField("点数", freqCountField));

        panel.add(Box.createVerticalStrut(10));
        panel.add(sectionLabel("弹体姿态 (deg)"));
        panel.add(labeledField("航向 (Yaw)", yawField));
        panel.add(labeledField("俯仰 (Pitch)", pitchField));
        panel.add(labeledField("横滚 (Roll)", rollField));

        panel.add(Box.createVerticalStrut(10));
        panel.add(sectionLabel("发射机经纬高"));
        panel.add(labeledField("纬度", txLatField));
        panel.add(labeledField("经度", txLonField));
        panel.add(labeledField("高度 (m)", txAltField));

        panel.add(Box.createVerticalStrut(10));
        panel.add(sectionLabel("接收机经纬高"));
        panel.add(labeledField("纬度", rxLatField));
        panel.add(labeledField("经度", rxLonField));
        panel.add(labeledField("高度 (m)", rxAltField));

        panel.add(Box.createVerticalStrut(10));
        panel.add(sectionLabel("目标经纬高"));
        panel.add(labeledField("纬度", tgtLatField));
        panel.add(labeledField("经度", tgtLonField));
        panel.add(labeledField("高度 (m)", tgtAltField));

        panel.add(Box.createVerticalStrut(10));
        JButton compute = new JButton("计算并绘图");
        compute.addActionListener(e -> updateResults());
        panel.add(compute);

        panel.add(Box.createVerticalStrut(10));
        panel.add(geometryLabel);

        return panel;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setBorder(BorderFactory.createEmptyBorder(6, 0, 2, 0));
        return label;
    }

    private JPanel labeledField(String label, JTextField field) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel(label), BorderLayout.WEST);
        p.add(field, BorderLayout.CENTER);
        field.setColumns(8);
        return p;
    }

    private void updateResults() {
        double freqStart = parseDouble(freqStartField.getText(), 8.0);
        double freqStop = parseDouble(freqStopField.getText(), 12.0);
        int freqPoints = (int) parseDouble(freqCountField.getText(), 40.0);

        Attitude attitude = new Attitude(parseDouble(yawField.getText(), 0.0),
                parseDouble(pitchField.getText(), 0.0),
                parseDouble(rollField.getText(), 0.0));

        GeoPoint tx = new GeoPoint(parseDouble(txLatField.getText(), 30.0),
                parseDouble(txLonField.getText(), -100.0),
                parseDouble(txAltField.getText(), 100.0));
        GeoPoint rx = new GeoPoint(parseDouble(rxLatField.getText(), 25.0),
                parseDouble(rxLonField.getText(), -90.0),
                parseDouble(rxAltField.getText(), 100.0));
        GeoPoint tgt = new GeoPoint(parseDouble(tgtLatField.getText(), 28.0),
                parseDouble(tgtLonField.getText(), -95.0),
                parseDouble(tgtAltField.getText(), 10000.0));

        RcsCalculator.BistaticGeometry geom = calculator.geometry(tx, rx, tgt, attitude);
        geometryLabel.setText(String.format("入射 az/el = %.1f/%.1f, 散射 az/el = %.1f/%.1f (deg)",
                geom.incidenceAzDeg(), geom.incidenceElDeg(), geom.scatterAzDeg(), geom.scatterElDeg()));

        RcsCalculator.SweepResult sweep = calculator.sweep(freqStart, freqStop, freqPoints,
                geom.incidenceAzDeg(), geom.incidenceElDeg(), geom.scatterAzDeg(), geom.scatterElDeg());
        lineChart.updateData(sweep.freqsGHz(), sweep.rcsValues());

        double[] azDeg = DoubleStream.iterate(-180.0, d -> d <= 180.0, d -> d + 5.0).toArray();
        double[] elDeg = DoubleStream.iterate(-60.0, d -> d <= 60.0, d -> d + 5.0).toArray();
        double[][] grid = calculator.directionGrid((freqStart + freqStop) * 0.5, azDeg, elDeg,
                geom.incidenceAzDeg(), geom.incidenceElDeg(), geom.scatterAzDeg(), geom.scatterElDeg());
        heatmap.updateData(azDeg, elDeg, grid, geom.incidenceAzDeg(), geom.incidenceElDeg(),
                geom.scatterAzDeg(), geom.scatterElDeg());
    }

    private double parseDouble(String text, double fallback) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RcsApp app = new RcsApp();
            app.setVisible(true);
        });
    }
}
