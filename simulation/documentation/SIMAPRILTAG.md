# SimAprilTag Documentation

## Overview

`SimAprilTag` is a simulated AprilTag detection system that mimics FTC robot vision behavior. It implements realistic camera constraints including field of view (FOV), range limits, and orientation tracking.

## Class Structure

```java
public class SimAprilTag implements AprilTagIO {
    public static class TagPose {
        public final double x;
        public final double y;
        public final double yaw;
        public final double range;
        public final boolean visible;
    }
}
```

## TagPose Representation

The detected tag pose contains:

- **x**: Tag X coordinate (inches)
- **y**: Tag Y coordinate (inches)
- **yaw**: Yaw angle relative to robot (radians)
- **range**: Distance from robot to tag (inches)
- **visible**: Whether tag is currently visible

## Camera Simulation

Simulates Logitech C270 webcam with realistic constraints:

```java
private static final double CAMERA_FOV_RAD = Math.toRadians(55.0);      // 55° horizontal FOV
private static final double MAX_RANGE_INCH = 120.0;                    // 120 inch max range
private static final double MAX_YAW_RAD = Math.toRadians(27.5);        // 27.5° max yaw (half FOV)
private static final double CAMERA_HEIGHT_INCH = 16.0;                // 16 inch camera height
private static final double TAG_HEIGHT_INCH = 23.0;                     // 23 inch tag height (pylon)
private static final double MAX_PITCH_ANGLE = Math.toRadians(20.0);    // 20° max pitch upward
```

### Visibility Conditions

Tag is visible if ALL conditions met:

1. **Enabled**: `visible` flag is true
2. **Range**: Distance ≤ 120 inches
3. **Yaw**: Absolute yaw ≤ 27.5° (within FOV)
4. **Pitch**: Pitch angle ≤ 20° (can look up at tag)

## Key Methods

### Constructor

```java
// Basic constructor (no rotation)
SimAprilTag tag = new SimAprilTag(1, 0.0, 72.0);

// With orientation
SimAprilTag tag = new SimAprilTag(1, 0.0, 72.0, Math.toRadians(45));
```

**Parameters**:
- `id`: Tag ID number
- `fieldX`: X coordinate on field (inches)
- `fieldY`: Y coordinate on field (inches)
- `orientationRadians`: Tag rotation angle (optional, default 0)

### `getId()`

Returns tag ID.

```java
int id = tag.getId();
```

### `getFieldX()` / `getFieldY()`

Returns tag position on field.

```java
double x = tag.getFieldX();
double y = tag.getFieldY();
```

### `getOrientation()`

Returns tag rotation angle.

```java
double orientation = tag.getOrientation();
```

### `setVisible(boolean v)`

Sets tag visibility.

```java
tag.setVisible(true);   // Enable tag
tag.setVisible(false);  // Disable tag
```

### `toggleVisible()`

Toggles tag visibility.

```java
tag.toggleVisible();
```

### `isVisible()`

Returns current visibility state (from `AprilTagIO` interface).

```java
if (tag.isVisible()) {
    // Tag is visible
}
```

### `yaw()`

Returns yaw angle relative to robot (from `AprilTagIO` interface).

```java
double yaw = tag.yaw();  // Radians
double yawDeg = Math.toDegrees(yaw);
```

### `range()`

Returns distance from robot to tag (from `AprilTagIO` interface).

```java
double range = tag.range();  // Inches
```

### `computePose(double robotX, double robotY, double robotHeadingRad)`

Computes tag pose relative to robot.

```java
SimAprilTag.TagPose pose = tag.computePose(robotX, robotY, robotHeadingRad);
```

**Parameters**:
- `robotX`: Robot X coordinate
- `robotY`: Robot Y coordinate
- `robotHeadingRad`: Robot heading (radians)

**Returns**: `TagPose` with current measurements

**Behavior**:
- Calculates distance to tag
- Calculates bearing to tag
- Computes yaw relative to robot heading
- Checks visibility conditions
- Updates internal measurements

### `adjustYawBias(double delta)`

Adjusts yaw measurement bias (simulates calibration error).

```java
tag.adjustYawBias(0.05);   // Add 0.05 rad bias
tag.adjustYawBias(-0.05);  // Subtract 0.05 rad bias
```

### `resetYawBias()`

Resets yaw bias to zero.

```java
tag.resetYawBias();
```

### `adjustYawRadians(double delta)`

Legacy method - same as `adjustYawBias()`.

```java
tag.adjustYawRadians(0.05);
```

### `adjustRangeInches(double delta)`

Legacy method - no-op (range is geometry-derived).

```java
tag.adjustRangeInches(2.0);  // Does nothing
```

### `getLastPose()`

Returns last computed tag pose.

```java
SimAprilTag.TagPose last = tag.getLastPose();
```

## Usage Examples

### Basic Usage

```java
// Create tag
SimAprilTag tag = new SimAprilTag(5, 0.0, 72.0);

// Check visibility
if (tag.isVisible()) {
    System.out.println("Tag visible");
    System.out.println("Yaw: " + Math.toDegrees(tag.yaw()) + "°");
    System.out.println("Range: " + tag.range() + " inches");
}
```

### Compute Pose

```java
// Robot at (15, -55) facing 45°
double robotX = 15.0;
double robotY = -55.0;
double robotHeading = Math.toRadians(45.0);

SimAprilTag.TagPose pose = tag.computePose(robotX, robotY, robotHeading);

if (pose.visible) {
    System.out.printf("Tag at (%.1f, %.1f)\n", pose.x, pose.y);
    System.out.printf("Yaw: %.2f°, Range: %.1f\"\n", 
        Math.toDegrees(pose.yaw), pose.range);
}
```

### Toggle Visibility

```java
// Toggle on/off
tag.toggleVisible();

// Explicit control
tag.setVisible(true);
tag.setVisible(false);
```

### Adjust Yaw Bias

```java
// Simulate calibration error
tag.adjustYawBias(0.1);  // Add 0.1 rad bias

// Reset
tag.resetYawBias();
```

### Multiple Tags

```java
// Create multiple tags
SimAprilTag tag4 = new SimAprilTag(4, -60.5, 62.0, Math.toRadians(45));
SimAprilTag tag5 = new SimAprilTag(5, 0.0, 72.0);
SimAprilTag tag6 = new SimAprilTag(6, 60.5, 62.0, Math.toRadians(-30));

// Check all tags
if (tag4.isVisible()) {
    System.out.println("Tag4 visible");
}
if (tag5.isVisible()) {
    System.out.println("Tag5 visible");
}
if (tag6.isVisible()) {
    System.out.println("Tag6 visible");
}
```

## Integration with AutoLogic

`SimAprilTag` implements `AprilTagIO` interface:

```java
AutoLogic autoLogic = new AutoLogic();
SimAprilTag tag21 = new SimAprilTag(5, 0.0, 72.0);
SimAprilTag tag24 = new SimAprilTag(6, 60.5, 62.0, Math.toRadians(-30));
SimDrive drive = new SimDrive();

// Main loop
while (running) {
    autoLogic.update(0.02, tag21, tag24, drive);
}
```

## Visibility Calculation

The `computePose()` method calculates visibility based on:

### 1. Distance Check

```java
double dx = fieldX - robotX;
double dy = fieldY - robotY;
double range = Math.hypot(dx, dy);
boolean inRange = range <= MAX_RANGE_INCH;
```

### 2. Yaw Check

```java
double absoluteBearing = Math.atan2(dy, dx);
double yawRel = normalize(absoluteBearing - robotHeadingRad + yawBiasRad);
boolean inFOV = Math.abs(yawRel) <= MAX_YAW_RAD;
```

### 3. Pitch Check

```java
double heightDiff = TAG_HEIGHT_INCH - CAMERA_HEIGHT_INCH;
double pitchAngle = Math.atan2(heightDiff, range);
boolean canSeeUp = pitchAngle <= MAX_PITCH_ANGLE;
```

### 4. Final Visibility

```java
boolean visible = enabled && inRange && inFOV && canSeeUp;
```

## Yaw Bias

Yaw bias simulates calibration error:

```java
private volatile double yawBiasRad = 0.0;

// Apply bias to yaw calculation
double yawRel = normalize(absoluteBearing - robotHeadingRad + yawBiasRad);
```

Use for testing error recovery:
```java
// Add bias to test alignment logic
tag.adjustYawBias(0.1);

// AutoLogic should compensate for small errors
```

## Coordinate System

Tag positions relative to field center:

```
         +Y (away from audience)
         |
    Tag4 |       Tag5       | Tag6
         |      (0,72)      |
---------+---------+---------+X
         |                 |
         |                 |
         -Y (toward audience)
```

## Real Robot Integration

Replace with hardware implementation:

```java
public class RobotAprilTag implements AprilTagIO {
    private AprilTagDetection detection;
    
    @Override
    public boolean isVisible() {
        return detection != null;
    }
    
    @Override
    public double yaw() {
        return detection.yaw;
    }
    
    @Override
    public double range() {
        return detection.range;
    }
}
```

## Debugging

### Check Tag Visibility

```java
SimAprilTag.TagPose pose = tag.computePose(robotX, robotY, robotHeading);
System.out.println("Visible: " + pose.visible);
System.out.println("Yaw: " + Math.toDegrees(pose.yaw) + "°");
System.out.println("Range: " + pose.range + " inches");
```

### Test Visibility Conditions

```java
// Test at different positions
double[][] testPositions = {
    {15.0, -55.0, 45.0},
    {0.0, 0.0, 0.0},
    {-30.0, 30.0, 90.0}
};

for (double[] pos : testPositions) {
    SimAprilTag.TagPose pose = tag.computePose(pos[0], pos[1], Math.toRadians(pos[2]));
    System.out.printf("At (%.1f, %.1f, %.0f°): %s\n",
        pos[0], pos[1], pos[2], pose.visible ? "Visible" : "Hidden");
}
```

### Monitor Yaw Bias

```java
System.out.println("Yaw bias: " + tag.yaw());
tag.adjustYawBias(0.05);
System.out.println("After adjustment: " + tag.yaw());
tag.resetYawBias();
System.out.println("After reset: " + tag.yaw());
```

## Common Issues

### Issue: Tag never visible

**Causes**:
1. Tag disabled
2. Out of range (>120 inches)
3. Out of FOV (>27.5° yaw)
4. Pitch too high (>20°)

**Solutions**:
```java
// Enable tag
tag.setVisible(true);

// Check distance
double range = tag.computePose(x, y, heading).range;
if (range > 120) {
    System.out.println("Too far away");
}

// Check yaw
double yaw = Math.toDegrees(tag.yaw());
if (Math.abs(yaw) > 27.5) {
    System.out.println("Out of FOV");
}
```

### Issue: Yaw measurements wrong

**Cause**: Yaw bias active

**Solution**:
```java
tag.resetYawBias();
```

### Issue: Tag visible but wrong range

**Cause**: Not calling `computePose()` before reading

**Solution**:
```java
// Must compute first
tag.computePose(robotX, robotY, robotHeading);
double range = tag.range();
```

## Performance Notes

- Computation: O(1) - simple trigonometry
- Thread-safe for single-threaded use
- No external dependencies
- Updates only when `computePose()` called

## Notes

- Yaw normalized to [-π, π] range
- Range is floor distance (not 3D distance)
- Orientation affects tag rotation display only
- Legacy methods (`adjustRangeInches`) kept for compatibility
