# SimDrive Documentation

## Overview

`SimDrive` is a simulated differential drive system that mimics FTC robot drive behavior. It provides position tracking, movement simulation, and implements the `DriveIO` interface.

## Class Structure

```java
public class SimDrive implements DriveIO {
    public static class Pose {
        public double x;
        public double y;
        public double headingRad;
    }
}
```

## Pose Representation

The robot's position is represented by a `Pose` object:

- **x**: X coordinate (inches) from field center
- **y**: Y coordinate (inches) from field center
- **headingRad**: Heading in radians (0 = facing +Y, positive = clockwise)

## Drive Modes

```java
private enum Mode { IDLE, DRIVE, ROTATE, TANK }
```

### IDLE
- No active movement
- Robot stationary
- Default state

### DRIVE
- Straight line movement
- Uses `driveStraight()` command
- Updates position based on heading

### ROTATE
- In-place rotation
- Uses `rotate()` command
- Updates heading only

### TANK
- Differential drive
- Uses `tank()` command
- Both linear and angular movement

## Key Methods

### `getPose()`

Returns current robot pose.

```java
SimDrive.Pose pose = drive.getPose();
double x = pose.x;
double y = pose.y;
double heading = Math.toDegrees(pose.headingRad);
```

### `setPose(double x, double y, double headingRadians)`

Sets robot to specific position and heading.

```java
drive.setPose(15.0, -55.0, Math.toRadians(45.0));
```

**Behavior**:
- Sets position immediately
- Resets mode to IDLE
- Clears remaining distance/angle
- Clears tank drive values

### `isBusy()`

Returns true if robot is currently moving.

```java
if (drive.isBusy()) {
    // Robot is moving
}
```

**Returns true for**: DRIVE, ROTATE modes
**Returns false for**: IDLE, TANK modes

### `tank(double left, double right)`

Sets differential drive power.

```java
drive.tank(0.5, 0.5);  // Forward
drive.tank(-0.5, -0.5); // Backward
drive.tank(0.3, -0.3);  // Rotate clockwise
drive.tank(-0.3, 0.3);  // Rotate counter-clockwise
```

**Parameters**:
- `left`: Left motor power (-1.0 to 1.0)
- `right`: Right motor power (-1.0 to 1.0)

**Behavior**:
- Sets mode to TANK
- Stores power values
- Applied in `update()`

### `driveStraight(double inches, double power)`

Drives straight for specified distance.

```java
drive.driveStraight(24.0, 0.8);  // Forward 24 inches
drive.driveStraight(-24.0, 0.8); // Backward 24 inches
```

**Parameters**:
- `inches`: Distance to drive (positive = forward, negative = backward)
- `power`: Motor power (-1.0 to 1.0)

**Behavior**:
- Calculates duration based on speed
- Sets mode to DRIVE
- Stores remaining distance and speed

### `rotate(double degrees, double power)`

Rotates in place by specified angle.

```java
drive.rotate(90.0, 0.9);   // Rotate 90° clockwise
drive.rotate(-90.0, 0.9);  // Rotate 90° counter-clockwise
```

**Parameters**:
- `degrees`: Angle to rotate (positive = clockwise, negative = counter-clockwise)
- `power`: Motor power (-1.0 to 1.0)

**Behavior**:
- Calculates duration based on speed
- Sets mode to ROTATE
- Stores remaining angle and speed
- **Note**: Reversed for FTC convention (negative degrees = clockwise)

### `shoot(int count)`

Simulates shooting balls (placeholder).

```java
drive.shoot(3);  // Shoot 3 balls
```

**Behavior**: No-op in simulation (placeholder)

### `collect(int batch)`

Simulates collecting balls (placeholder).

```java
drive.collect(1);  // Collect batch 1
```

**Behavior**: No-op in simulation (placeholder)

### `update(double dtSeconds)`

Updates robot position based on current mode.

```java
drive.update(0.02);  // Called every 20ms
```

**Parameters**:
- `dtSeconds`: Delta time in seconds

**Behavior**:
- DRIVE: Updates position along heading
- ROTATE: Updates heading
- TANK: Updates both position and heading
- IDLE: No change

### `stop()`

Stops all movement.

```java
drive.stop();
```

**Behavior**:
- Sets mode to IDLE
- Clears tank drive values

## Movement Physics

### Straight Drive

```
position += speed * dt * cos(heading)
position += speed * dt * sin(heading)
```

Where:
- `speed = |power| * maxLinearSpeed`
- `maxLinearSpeed = 40.0 inches/second`

### Rotation

```
heading += rotationSpeed * dt
```

Where:
- `rotationSpeed = |power| * angularSpeedDegPerSec`
- `angularSpeedDegPerSec = 360.0 degrees/second`

### Tank Drive

```
linear = (vLeft + vRight) / 2
angular = (vRight - vLeft) / trackWidth
x += linear * dt * cos(heading)
y += linear * dt * sin(heading)
heading += angular * dt
```

Where:
- `vLeft = leftPower * maxLinearSpeed`
- `vRight = rightPower * maxLinearSpeed`
- `trackWidth = 16.0 inches`

## Configuration Parameters

```java
private final double maxLinearSpeed = 40.0;    // inches per second
private final double trackWidth = 16.0;         // inches
```

### Max Linear Speed

Maximum speed for straight movement.

**Default**: 40.0 inches/second
**Adjust**: Modify in `SimDrive.java`

### Track Width

Distance between wheels for tank drive calculations.

**Default**: 16.0 inches
**Adjust**: Modify in `SimDrive.java`

## Usage Examples

### Basic Movement

```java
SimDrive drive = new SimDrive();

// Set initial position
drive.setPose(15.0, -55.0, Math.toRadians(45.0));

// Drive forward 24 inches
drive.driveStraight(24.0, 0.8);

// Update in loop
while (drive.isBusy()) {
    drive.update(0.02);
    // ... other updates
}

// Rotate 90 degrees
drive.rotate(90.0, 0.9);

while (drive.isBusy()) {
    drive.update(0.02);
}
```

### Tank Drive

```java
// Continuous tank control
while (true) {
    drive.tank(leftPower, rightPower);
    drive.update(0.02);
    // ... other updates
}
```

### Position Tracking

```java
// Get current position
SimDrive.Pose pose = drive.getPose();
System.out.printf("Position: x=%.1f, y=%.1f, heading=%.1f\n",
    pose.x, pose.y, Math.toDegrees(pose.headingRad));
```

### Stop and Reset

```java
// Stop movement
drive.stop();

// Reset position
drive.setPose(0.0, 0.0, 0.0);
```

## Integration with AutoLogic

`SimDrive` implements `DriveIO` interface, making it compatible with `AutoLogic`:

```java
AutoLogic autoLogic = new AutoLogic();
SimDrive drive = new SimDrive();

// Main loop
while (running) {
    autoLogic.update(0.02, tag21, tag24, drive);
    drive.update(0.02);
}
```

## Real Robot Integration

To use on real robot, replace `SimDrive` with hardware implementation:

```java
// On robot
public class RobotDrive implements DriveIO {
    private DcMotor leftMotor;
    private DcMotor rightMotor;
    
    @Override
    public void tank(double left, double right) {
        leftMotor.setPower(left);
        rightMotor.setPower(right);
    }
    
    // ... implement other methods
}
```

Keep `AutoLogic` unchanged - it uses `DriveIO` interface.

## Debugging

### Check Current State

```java
SimDrive.Pose pose = drive.getPose();
System.out.println("X: " + pose.x);
System.out.println("Y: " + pose.y);
System.out.println("Heading: " + Math.toDegrees(pose.headingRad));
System.out.println("Busy: " + drive.isBusy());
```

### Monitor Movement

```java
// Before movement
SimDrive.Pose start = drive.getPose();

// Execute movement
drive.driveStraight(24.0, 0.8);
while (drive.isBusy()) {
    drive.update(0.02);
}

// After movement
SimDrive.Pose end = drive.getPose();
double distance = Math.hypot(end.x - start.x, end.y - start.y);
System.out.println("Traveled: " + distance + " inches");
```

## Performance Notes

- Update rate: 50Hz (20ms dt)
- Position tracking: Sub-millisecond precision
- No external dependencies
- Thread-safe for single-threaded use

## Common Issues

### Issue: Robot doesn't move

**Cause**: Not calling `update()`

**Solution**:
```java
// Must call update() in loop
while (drive.isBusy()) {
    drive.update(0.02);
}
```

### Issue: Movement too fast/slow

**Cause**: Power or speed settings

**Solution**:
```java
// Adjust power (0.0 to 1.0)
drive.driveStraight(24.0, 0.5);  // Slower

// Or adjust maxLinearSpeed
private final double maxLinearSpeed = 30.0;  // Slower max
```

### Issue: Rotation wrong direction

**Cause**: FTC convention (negative = clockwise)

**Solution**:
```java
// Use negative for clockwise
drive.rotate(-90.0, 0.9);  // Clockwise
drive.rotate(90.0, 0.9);   // Counter-clockwise
```

## Notes

- Heading normalized to [-π, π] range
- Position updates use trigonometry for accuracy
- Tank drive simulates differential drive physics
- Compatible with real robot hardware via interface
