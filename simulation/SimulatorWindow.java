import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class SimulatorWindow extends JFrame {
    private static final double FIELD_SIZE_IN = 144.0;
    private static final String DEFAULT_FIELD_PATH = "S:\\\\FTC\\\\simulation\\\\field\\\\Screenshot 2026-01-09 144014.png";
    private final SimDrive drive = new SimDrive();
    // Default official FTC field AprilTag layout (Centerstage style, origin = field center, +x audience right, +y away from audience)
    private final SimAprilTag tag1 = new SimAprilTag(1, -35.5, -72.0);
    private final SimAprilTag tag2 = new SimAprilTag(2, 0.0, -72.0);
    private final SimAprilTag tag3 = new SimAprilTag(3, 35.5, -72.0);
    private final SimAprilTag tag4 = new SimAprilTag(4, -35.5, 72.0);
    private final SimAprilTag tag5 = new SimAprilTag(5, 0.0, 72.0);
    private final SimAprilTag tag6 = new SimAprilTag(6, 35.5, 72.0);
    // Legacy aliases to keep existing auto logic unchanged (uses two tags)
    private final SimAprilTag tag21 = tag2;
    private final SimAprilTag tag24 = tag5;
    private SimAprilTag controlTag = tag21;

    private final AutoLogic autoLogic = new AutoLogic();
    private boolean autoEnabled = false;
    private AutoLogic.AutoState lastState = autoLogic.getCurrentState();

    private final List<double[]> trail = new ArrayList<>();
    private final List<String> recentTransitions = new ArrayList<>();
    private final Timer timer;
    private double currentScale = 5.0; // pixels per inch, recalculated to fit field
    private final FieldPanel fieldPanel = new FieldPanel();
    private final JPanel controlPanel = new JPanel();
    private final JPanel infoPanel = new JPanel();
    private final JTextField startXField = new JTextField("-12", 4);
    private final JTextField startYField = new JTextField("-60", 4);
    private final JTextField startHeadingField = new JTextField("90", 4);
    private final JTextArea hudArea = new JTextArea();
    private BufferedImage fieldImage = null;
    private void loadDefaultFieldImage() {
        try {
            fieldImage = ImageIO.read(new File(DEFAULT_FIELD_PATH));
        } catch (IOException ex) {
            // Keep null if loading fails; user can load manually
            fieldImage = null;
        }
    }

    public SimulatorWindow() {
        super("FTC Autonomous Simulator");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1120, 860);
        setMinimumSize(new Dimension(1120, 860));
        setMaximumSize(new Dimension(1120, 860));
        setResizable(false);
        setLocationRelativeTo(null);
        setBackground(Color.BLACK);
        setLayout(new BorderLayout());
        add(controlPanel, BorderLayout.NORTH);
        add(fieldPanel, BorderLayout.CENTER);
        add(infoPanel, BorderLayout.EAST);

        loadDefaultFieldImage();
        buildControls();
        buildInfoPanel();
        applyStartPose(); // initialize robot pose from defaults

        setupKeyBindings();

        timer = new Timer(20, this::tick);
        timer.start();
    }

    private void setupKeyBindings() {
        JComponent root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        im.put(KeyStroke.getKeyStroke("ENTER"), "toggleAuto");
        am.put("toggleAuto", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                autoEnabled = !autoEnabled;
                if (autoEnabled) {
                    autoLogic.reset();
                } else {
                    drive.stop();
                }
                repaint();
            }
        });

        im.put(KeyStroke.getKeyStroke("1"), "toggleTag21");
        am.put("toggleTag21", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tag21.toggleVisible();
                controlTag = tag21;
                repaint();
            }
        });

        im.put(KeyStroke.getKeyStroke("2"), "toggleTag24");
        am.put("toggleTag24", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tag24.toggleVisible();
                controlTag = tag24;
                repaint();
            }
        });

        im.put(KeyStroke.getKeyStroke("Q"), "yawLeft");
        am.put("yawLeft", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controlTag.adjustYawRadians(-0.05);
                repaint();
            }
        });

        im.put(KeyStroke.getKeyStroke("E"), "yawRight");
        am.put("yawRight", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controlTag.adjustYawRadians(0.05);
                repaint();
            }
        });

        im.put(KeyStroke.getKeyStroke("W"), "rangeIn");
        am.put("rangeIn", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controlTag.adjustRangeInches(-2.0);
                repaint();
            }
        });

        im.put(KeyStroke.getKeyStroke("S"), "rangeOut");
        am.put("rangeOut", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controlTag.adjustRangeInches(2.0);
                repaint();
            }
        });
    }

    private void tick(ActionEvent e) {
        double dt = 0.02;
        if (autoEnabled) {
            autoLogic.update(dt, tag21, tag24, drive);
        }
        drive.update(dt);
        SimDrive.Pose p = drive.getPose();
        trail.add(new double[]{p.x, p.y});
        if (trail.size() > 2000) trail.remove(0);

        AutoLogic.AutoState current = autoLogic.getCurrentState();
        if (current != lastState) {
            recentTransitions.add(0, String.format("%s -> %s", lastState, current));
            if (recentTransitions.size() > 6) recentTransitions.remove(recentTransitions.size() - 1);
            lastState = current;
        }
        updateHudText();
        repaint();
    }

    private void buildControls() {
        controlPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(new JLabel("Start X (in):"));
        controlPanel.add(startXField);
        controlPanel.add(new JLabel("Start Y (in):"));
        controlPanel.add(startYField);
        controlPanel.add(new JLabel("Heading (deg):"));
        controlPanel.add(startHeadingField);

        JButton spawnBtn = new JButton("Set Pose");
        spawnBtn.addActionListener(e -> applyStartPose());
        controlPanel.add(spawnBtn);

        JButton resetTrailBtn = new JButton("Reset Trail");
        resetTrailBtn.addActionListener(e -> {
            trail.clear();
            repaint();
        });
        controlPanel.add(resetTrailBtn);

        JButton loadFieldBtn = new JButton("Load Field Image");
        loadFieldBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int res = chooser.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                try {
                    fieldImage = javax.imageio.ImageIO.read(f);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Failed to load image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        controlPanel.add(loadFieldBtn);
    }

    private void buildInfoPanel() {
        infoPanel.setLayout(new BorderLayout());
        infoPanel.setPreferredSize(new Dimension(280, 0));
        hudArea.setEditable(false);
        hudArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        hudArea.setBackground(new Color(20, 20, 20));
        hudArea.setForeground(new Color(235, 235, 235));
        hudArea.setMargin(new Insets(10, 10, 10, 10));
        hudArea.setLineWrap(true);
        hudArea.setWrapStyleWord(true);
        hudArea.setRows(18);
        hudArea.setColumns(22);
        infoPanel.add(hudArea, BorderLayout.CENTER);
    }

    private void applyStartPose() {
        try {
            double x = Double.parseDouble(startXField.getText());
            double y = Double.parseDouble(startYField.getText());
            double headingDeg = Double.parseDouble(startHeadingField.getText());
            drive.setPose(x, y, Math.toRadians(headingDeg));
            trail.clear();
            autoLogic.reset();
            repaint();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid start pose numbers", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class FieldPanel extends JPanel {
        FieldPanel() {
            setDoubleBuffered(true);
            setBackground(Color.BLACK);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            g2.setColor(new Color(20, 20, 20));
            g2.fillRect(0, 0, w, h);

            updateScale(w, h);
            drawFieldBackground(g2, w, h);
            drawFieldGrid(g2, w, h);
            drawTrail(g2);
            drawTags(g2);
            drawRobot(g2);
        }
    }

    private void drawFieldBackground(Graphics2D g2, int w, int h) {
        if (fieldImage != null) {
            g2.drawImage(fieldImage, 0, 0, w, h, null);
        }
    }

    private void updateScale(int w, int h) {
        currentScale = Math.min(w, h) / FIELD_SIZE_IN;
    }

    private void drawFieldGrid(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(40, 40, 40));
        int spacing = (int) (12 * currentScale);
        for (int x = 0; x < w; x += spacing) {
            g2.drawLine(x, 0, x, h);
        }
        for (int y = 0; y < h; y += spacing) {
            g2.drawLine(0, y, w, y);
        }
    }

    private void drawTrail(Graphics2D g2) {
        g2.setColor(new Color(0, 180, 255, 120));
        int n = trail.size();
        for (int i = 1; i < n; i++) {
            double[] a = trail.get(i - 1);
            double[] b = trail.get(i);
            g2.drawLine(toScreenX(a[0]), toScreenY(a[1]), toScreenX(b[0]), toScreenY(b[1]));
        }
    }

    private void drawTags(Graphics2D g2) {
        drawTag(g2, tag1, new Color(70, 180, 255));
        drawTag(g2, tag2, new Color(80, 200, 140));
        drawTag(g2, tag3, new Color(200, 160, 90));
        drawTag(g2, tag4, new Color(255, 120, 120));
        drawTag(g2, tag5, new Color(160, 120, 255));
        drawTag(g2, tag6, new Color(220, 200, 120));
    }

    private void drawTag(Graphics2D g2, SimAprilTag tag, Color color) {
        SimDrive.Pose pose = drive.getPose();
        SimAprilTag.TagPose tp = tag.computePose(pose.x, pose.y, pose.headingRad);
        int size = (int) (6 * currentScale);
        int x = toScreenX(tp.x) - size / 2;
        int y = toScreenY(tp.y) - size / 2;
        g2.setColor(tp.visible ? color : new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
        g2.fillRect(x, y, size, size);
        g2.setColor(Color.WHITE);
        g2.drawRect(x, y, size, size);

        double yawGlobal = pose.headingRad + tag.yaw();
        int cx = toScreenX(tp.x);
        int cy = toScreenY(tp.y);
        int arrowLen = (int) (8 * currentScale);
        int ex = cx + (int) (Math.cos(yawGlobal) * arrowLen);
        int ey = cy - (int) (Math.sin(yawGlobal) * arrowLen);
        g2.drawLine(cx, cy, ex, ey);

        if (tp.visible) {
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 160));
            g2.drawLine(toScreenX(pose.x), toScreenY(pose.y), cx, cy);
            g2.setColor(Color.WHITE);
            g2.drawString(String.format("ID %d r=%.1f\" yaw=%.1f°", tag.getId(), tag.range(), Math.toDegrees(tag.yaw())),
                    cx + 6, cy - 6);
        }
    }

    private void drawRobot(Graphics2D g2) {
        SimDrive.Pose p = drive.getPose();
        int size = (int) (18 * currentScale);
        int cx = toScreenX(p.x);
        int cy = toScreenY(p.y);
        
        // Tambahkan rotasi ke kotak
        Graphics2D g2d = (Graphics2D) g2.create();
        g2d.translate(cx, cy);  // Pindah ke center robot
        g2d.rotate(-p.headingRad);  // Rotasi sesuai heading
        g2d.setColor(new Color(80, 160, 255));
        g2d.fillRect(-size/2, -size/2, size, size);  // Gambar kotak dari center
        g2d.setColor(Color.WHITE);
        g2d.drawRect(-size/2, -size/2, size, size);
        g2d.dispose();  // Cleanup

        int nose = (int) (size * 0.6);
        int nx = cx + (int) (Math.cos(p.headingRad) * nose);
        int ny = cy - (int) (Math.sin(p.headingRad) * nose);
        g2.drawLine(cx, cy, nx, ny);
    }

    private void updateHudText() {
        SimDrive.Pose p = drive.getPose();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Auto: %s\nState: %s\n", autoEnabled ? "ENABLED" : "DISABLED", autoLogic.getCurrentState().name()));
        sb.append(String.format("Pose: x=%.1f in, y=%.1f in, heading=%.1f deg\n", p.x, p.y, Math.toDegrees(p.headingRad)));
        sb.append(String.format("Tag21 vis=%s yaw=%.2f rad r=%.1f\n", tag21.isVisible(), tag21.yaw(), tag21.range()));
        sb.append(String.format("Tag24 vis=%s yaw=%.2f rad r=%.1f\n", tag24.isVisible(), tag24.yaw(), tag24.range()));
        sb.append("Controls: ENTER auto | 1 Tag21 | 2 Tag24 | Q/E yaw | W/S range | Set Pose button\n");
        sb.append("\nRecent state transitions:\n");
        for (String s : recentTransitions) {
            sb.append(s).append("\n");
        }
        hudArea.setText(sb.toString());
    }

    private int toScreenX(double xInches) {
        return (int) (fieldPanel.getWidth() / 2 + xInches * currentScale);
    }

    private int toScreenY(double yInches) {
        return (int) (fieldPanel.getHeight() / 2 - yInches * currentScale);
    }
}
