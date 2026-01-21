# SimulatorWindow Documentation

## Overview

`SimulatorWindow` is the main GUI class for the FTC Autonomous Simulator. It provides a visual interface for testing autonomous logic with field visualization, robot tracking, and real-time controls.

## Window Layout

```
┌─────────────────────────────────────────────────────────┐
│ Control Panel (Top)                                      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│                    Field Panel                           │
│                   (Center)                              │
│                                                         │
├─────────────────────────────────────────────────────────┤
│ Info Panel (Right)                                       │
└─────────────────────────────────────────────────────────┘
```

### Control Panel (Top)
- Start position controls
- Saved positions management
- Field image loading

### Field Panel (Center)
- Field visualization
- Robot display
- AprilTag display
- Trail visualization

### Info Panel (Right)
- Real-time HUD
- State transitions
- Tag information

## Key Components

### Field Image

```java
private BufferedImage fieldImage = null;
private static final String DEFAULT_FIELD_PATH = "S:\\FTC\\simulation\\field\\Screenshot 2026-01-09 144014.png";
```

**Behavior**:
- Loads default field image on startup
- Can load custom image via button
- Displays as background in FieldPanel

### Robot Drive

```java
private final SimDrive drive = new SimDrive();
```

**Behavior**:
- Simulated differential drive
- Position tracking
- Movement simulation

### AprilTags

```java
private final SimAprilTag tag4 = new SimAprilTag(4, -60.5, 62.0, Math.toRadians(45));
private final SimAprilTag tag5 = new SimAprilTag(5, 0.0, 72.0);
private final SimAprilTag tag6 = new SimAprilTag(6, 60.5, 62.0, Math.toRadians(-30));

// Legacy aliases
private final SimAprilTag tag21 = tag5;
private final SimAprilTag tag24 = tag6;
private SimAprilTag controlTag = tag21;
```

**Behavior**:
- Three active tags (4, 5, 6)
- Legacy aliases for backward compatibility
- `controlTag` tracks which tag is currently selected

### Auto Logic

```java
private final AutoLogic autoLogic = new AutoLogic();
private boolean autoEnabled = false;
```

**Behavior**:
- Manages autonomous state machine
- Enabled/disabled via ENTER key
- Resets when disabled

### Trail

```java
private final List<double[]> trail = new ArrayList<>();
```

**Behavior**:
- Stores robot path history
- Limited to 2000 points
- Displayed as blue line

### Saved Positions

```java
private final List<SavedPosition> savedPositions = new ArrayList<>();
private final JComboBox<SavedPosition> positionComboBox = new JComboBox<>();
```

**Behavior**:
- Saves/loads robot positions
- Persisted to `saved_positions.dat`
- Managed via GUI controls

## Main Loop

```java
private void tick(ActionEvent e) {
    double dt = 0.02;
    if (autoEnabled) {
        autoLogic.update(dt, tag21, tag24, drive);
    }
    drive.update(dt);
    // ... update trail, transitions, HUD
    repaint();
}
```

**Behavior**:
- Called every 20ms (50Hz)
- Updates auto logic if enabled
- Updates drive system
- Updates trail
- Updates HUD
- Repaints window

## Key Methods

### `loadDefaultFieldImage()`

Loads default field background.

```java
private void loadDefaultFieldImage() {
    try {
        fieldImage = ImageIO.read(new File(DEFAULT_FIELD_PATH));
    } catch (IOException ex) {
        fieldImage = null;
    }
}
```

### `buildControls()`

Creates control panel UI.

**Components**:
- Start position text fields (X, Y, Heading)
- Set Pose button
- Reset Trail button
- Load Field Image button
- Saved positions dropdown
- Save/Load/Delete buttons

### `buildInfoPanel()`

Creates info panel UI.

**Components**:
- JTextArea for HUD display
- Dark theme styling
- Fixed size (280px width)

### `applyStartPose()`

Applies start position from text fields.

```java
private void applyStartPose() {
    double x = Double.parseDouble(startXField.getText());
    double y = Double.parseDouble(startYField.getText());
    double headingDeg = Double.parseDouble(startHeadingField.getText());
    drive.setPose(x, y, Math.toRadians(headingDeg));
    trail.clear();
    autoLogic.reset();
    repaint();
}
```

### `setupKeyBindings()`

Sets up keyboard controls.

**Key Bindings**:
- ENTER: Toggle auto
- 1: Toggle Tag21 visibility
- 2: Toggle Tag24 visibility
- Q: Decrease yaw
- E: Increase yaw
- W: Decrease range
- S: Increase range

### `updateHudText()`

Updates HUD text display.

```java
private void updateHudText() {
    SimDrive.Pose p = drive.getPose();
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("Auto: %s\n", autoEnabled ? "ENABLED" : "DISABLED"));
    sb.append(String.format("State: %s\n", autoLogic.getCurrentState().name()));
    sb.append(String.format("Pose: x=%.1f in, y=%.1f in, heading=%.1f deg\n",
        p.x, p.y, Math.toDegrees(p.headingRad)));
    sb.append(String.format("Tag21 vis=%s yaw=%.2f rad r=%.1f\n",
        tag21.isVisible(), tag21.yaw(), tag21.range()));
    sb.append(String.format("Tag24 vis=%s yaw=%.2f rad r=%.1f\n",
        tag24.isVisible(), tag24.yaw(), tag24.range()));
    sb.append("Controls: ENTER auto | 1 Tag21 | 2 Tag24 | Q/E yaw | W/S range\n");
    // ... add state transitions
    hudArea.setText(sb.toString());
}
```

### `toScreenX(double xInches)` / `toScreenY(double yInches)`

Converts field coordinates to screen coordinates.

```java
private int toScreenX(double xInches) {
    return (int) (fieldPanel.getWidth() / 2 + xInches * currentScale);
}

private int toScreenY(double yInches) {
    return (int) (fieldPanel.getHeight() / 2 - yInches * currentScale);
}
```

**Behavior**:
- Translates origin to screen center
- Scales by `currentScale` (pixels per inch)
- Flips Y axis (screen Y is inverted)

### `updateScale(int w, int h)`

Calculates scale to fit field in window.

```java
private void updateScale(int w, int h) {
    currentScale = Math.min(w, h) / FIELD_SIZE_IN;
}
```

**Behavior**:
- Ensures field fits in window
- Maintains aspect ratio
- Recalculated on resize

## Drawing Methods

### `drawFieldBackground(Graphics2D g2, int w, int h)`

Draws field background image.

```java
private void drawFieldBackground(Graphics2D g2, int w, int h) {
    if (fieldImage != null) {
        g2.drawImage(fieldImage, 0, 0, w, h, null);
    }
}
```

### `drawFieldGrid(Graphics2D g2, int w, int h)`

Draws grid overlay.

```java
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
```

### `drawTrail(Graphics2D g2)`

Draws robot path.

```java
private void drawTrail(Graphics2D g2) {
    g2.setColor(new Color(0, 180, 255, 120));
    int n = trail.size();
    for (int i = 1; i < n; i++) {
        double[] a = trail.get(i - 1);
        double[] b = trail.get(i);
        g2.drawLine(toScreenX(a[0]), toScreenY(a[1]),
                   toScreenX(b[0]), toScreenY(b[1]));
    }
}
```

### `drawTags(Graphics2D g2)`

Draws all AprilTags.

```java
private void drawTags(Graphics2D g2) {
    drawTag(g2, tag4, new Color(255, 120, 120));
    drawTag(g2, tag5, new Color(160, 120, 255));
    drawTag(g2, tag6, new Color(220, 200, 120));
}
```

### `drawTag(Graphics2D g2, SimAprilTag tag, Color color)`

Draws individual AprilTag.

```java
private void drawTag(Graphics2D g2, SimAprilTag tag, Color color) {
    SimDrive.Pose pose = drive.getPose();
    SimAprilTag.TagPose tp = tag.computePose(pose.x, pose.y, pose.headingRad);
    int size = (int) (6 * currentScale);
    int cx = toScreenX(tp.x);
    int cy = toScreenY(tp.y);
    
    // Draw rotated tag
    Graphics2D g2d = (Graphics2D) g2.create();
    g2d.translate(cx, cy);
    g2d.rotate(tag.getOrientation());
    g2d.setColor(tp.visible ? color : new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
    g2d.fillRect(-size/2, -size/2, size, size);
    g2d.setColor(Color.WHITE);
    g2d.drawRect(-size/2, -size/2, size, size);
    g2d.dispose();
    
    // Draw line to robot if visible
    if (tp.visible) {
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 160));
        g2.drawLine(toScreenX(pose.x), toScreenY(pose.y), cx, cy);
        g2.setColor(Color.WHITE);
        g2.drawString(String.format("ID %d r=%.1f\" yaw=%.1f°", tag.getId(), tag.range(), Math.toDegrees(tag.yaw())),
                cx + 6, cy - 6);
    }
}
```

### `drawRobot(Graphics2D g2)`

Draws robot.

```java
private void drawRobot(Graphics2D g2) {
    SimDrive.Pose p = drive.getPose();
    int size = (int) (18 * currentScale);
    int cx = toScreenX(p.x);
    int cy = toScreenY(p.y);
    
    // Draw rotated robot
    Graphics2D g2d = (Graphics2D) g2.create();
    g2d.translate(cx, cy);
    g2d.rotate(-p.headingRad);
    g2d.setColor(new Color(80, 160, 255));
    g2d.fillRect(-size/2, -size/2, size, size);
    g2d.setColor(Color.WHITE);
    g2d.drawRect(-size/2, -size/2, size, size);
    g2d.dispose();
    
    // Draw heading indicator
    int nose = (int) (size * 0.6);
    int nx = cx + (int) (Math.cos(p.headingRad) * nose);
    int ny = cy - (int) (Math.sin(p.headingRad) * nose);
    g2.drawLine(cx, cy, nx, ny);
}
```

## Position Management

### `saveCurrentPosition()`

Saves current robot position.

```java
private void saveCurrentPosition() {
    SimDrive.Pose p = drive.getPose();
    String name = JOptionPane.showInputDialog(this, "Enter position name:", "Save Position", JOptionPane.QUESTION_MESSAGE);
    if (name != null && !name.trim().isEmpty()) {
        SavedPosition pos = new SavedPosition(name.trim(), p.x, p.y, Math.toDegrees(p.headingRad));
        savedPositions.add(pos);
        positionComboBox.addItem(pos);
        savePositionsToFile();
    }
}
```

### `loadSelectedPosition()`

Loads selected position.

```java
private void loadSelectedPosition() {
    SavedPosition selected = (SavedPosition) positionComboBox.getSelectedItem();
    if (selected != null) {
        drive.setPose(selected.getX(), selected.getY(), Math.toRadians(selected.getHeading()));
        startXField.setText(String.format("%.1f", selected.getX()));
        startYField.setText(String.format("%.1f", selected.getY()));
        startHeadingField.setText(String.format("%.1f", selected.getHeading()));
        trail.clear();
        autoLogic.reset();
        repaint();
    }
}
```

### `deleteSelectedPosition()`

Deletes selected position.

```java
private void deleteSelectedPosition() {
    SavedPosition selected = (SavedPosition) positionComboBox.getSelectedItem();
    if (selected != null) {
        int result = JOptionPane.showConfirmDialog(this, "Delete position: " + selected.getName() + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            savedPositions.remove(selected);
            positionComboBox.removeItem(selected);
            savePositionsToFile();
        }
    }
}
```

### `loadSavedPositions()` / `savePositionsToFile()`

Loads/saves positions to file.

```java
private void loadSavedPositions() {
    File file = new File("saved_positions.dat");
    if (file.exists()) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            @SuppressWarnings("unchecked")
            List<SavedPosition> loaded = (List<SavedPosition>) ois.readObject();
            savedPositions.clear();
            positionComboBox.removeAllItems();
            for (SavedPosition pos : loaded) {
                savedPositions.add(pos);
                positionComboBox.addItem(pos);
            }
        } catch (IOException | ClassNotFoundException ex) {
            System.err.println("Failed to load saved positions: " + ex.getMessage());
        }
    }
}

private void savePositionsToFile() {
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("saved_positions.dat"))) {
        oos.writeObject(savedPositions);
    } catch (IOException ex) {
        System.err.println("Failed to save positions: " + ex.getMessage());
    }
}
```

## Configuration

### Window Size

```java
setSize(1120, 900);
setMinimumSize(new Dimension(1120, 900));
setMaximumSize(new Dimension(1120, 900));
setResizable(false);
```

### Field Size

```java
private static final double FIELD_SIZE_IN = 144.0;
```

### Update Rate

```java
timer = new Timer(20, this::tick);  // 20ms = 50Hz
```

## Usage Examples

### Creating Custom Window

```java
public class MySimulatorWindow extends JFrame {
    public MySimulatorWindow() {
        super("My FTC Simulator");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1280, 1024);
        // ... custom setup
    }
}
```

### Adding Custom Controls

```java
private void buildControls() {
    // Add custom button
    JButton myButton = new JButton("My Action");
    myButton.addActionListener(e -> {
        // Custom action
    });
    controlPanel.add(myButton);
}
```

### Custom Drawing

```java
private void drawCustomOverlay(Graphics2D g2) {
    g2.setColor(Color.RED);
    g2.drawString("Custom Text", 100, 100);
}

// Call in paintComponent
@Override
protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;
    // ... standard drawing
    drawCustomOverlay(g2);
}
```

## Debugging

### Check Window State

```java
System.out.println("Auto enabled: " + autoEnabled);
System.out.println("Current state: " + autoLogic.getCurrentState());
System.out.println("Window size: " + getSize());
```

### Monitor Performance

```java
long startTime = System.currentTimeMillis();
// ... operations
long elapsed = System.currentTimeMillis() - startTime;
System.out.println("Frame time: " + elapsed + "ms");
```

### Test Drawing

```java
// Add temporary drawing
g2.setColor(Color.RED);
g2.drawString("DEBUG", 100, 100);
```

## Common Issues

### Issue: Field image not loading

**Cause**: File path incorrect or missing

**Solution**:
```java
// Load manually via button
JFileChooser chooser = new JFileChooser();
int res = chooser.showOpenDialog(this);
if (res == JFileChooser.APPROVE_OPTION) {
    File f = chooser.getSelectedFile();
    fieldImage = ImageIO.read(f);
}
```

### Issue: Trail not updating

**Cause**: Not calling `repaint()`

**Solution**:
```java
trail.clear();
repaint();
```

### Issue: HUD not updating

**Cause**: Not calling `updateHudText()`

**Solution**:
```java
updateHudText();
repaint();
```

## Notes

- Window is fixed size (1120x900)
- Uses Swing double-buffering
- 50Hz update rate
- Trail limited to 2000 points
- Positions saved to `saved_positions.dat`
- Field image optional (can use grid only)
