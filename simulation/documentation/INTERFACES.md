# Interfaces Documentation

## Overview

This document describes the core interfaces used in the FTC Autonomous Simulator. These interfaces enable separation between simulation and real robot hardware.

## DriveIO Interface

### Purpose

`DriveIO` defines the contract for robot drive operations, allowing both simulated and real robot implementations.

### Location

`@/s:\FTC\simulation\DriveIO.java:1-8`

### Interface Definition

```java
public interface DriveIO {
    void tank(double left, double right);
    void driveStraight(double inches, double power);
    void rotate(double degrees, double power);
    void shoot(int count);
    void collect(int batch);
}
```

### Methods

#### `tank(double left, double right)`

Direct differential drive control.

**Parameters**:
- `left`: Left motor power (-1.0 to 1.0)
- **right**: Right motor power (-1.0 to 1.0)

**Usage**:
```java
drive.tank(0.5, 0.5);   // Forward
drive.tank(-0.5, -0.5); // Backward
drive.tank(0.3, -0.3);  // Rotate clockwise
```

#### `driveStraight(double inches, double power)`

Drive in a straight line for specified distance.

**Parameters**:
- `inches`: Distance to drive (positive = forward, negative = backward)
- `power`: Motor power (-1.0 to 1.0)

**Usage**:
```java
drive.driveStraight(24.0, 0.8);  // Forward 24 inches
drive.driveStraight(-24.0, 0.8); // Backward 24 inches
```

#### `rotate(double degrees, double power)`

Rotate in place by specified angle.

**Parameters**:
- `degrees`: Angle to rotate (positive = clockwise, negative = counter-clockwise)
- `power`: Motor power (-1.0 to 1.0)

**Usage**:
```java
drive.rotate(90.0, 0.9);   // Rotate 90° clockwise
drive.rotate(-90.0, 0.9);  // Rotate 90° counter-clockwise
```

#### `shoot(int count)`

Shoot balls into target.

**Parameters**:
- `count`: Number of balls to shoot

**Usage**:
```java
drive.shoot(3);  // Shoot 3 balls
```

#### `collect(int batch)`

Collect balls from field.

**Parameters**:
- `batch`: Batch number (for tracking collection cycles)

**Usage**:
```java
drive.collect(1);  // Collect first batch
drive.collect(2);  // Collect second batch
```

### Implementations

#### SimDrive

Simulated differential drive for testing.

```java
public class SimDrive implements DriveIO {
    // ... implementation
}
```

**Features**:
- Position tracking (x, y, heading)
- Movement simulation
- Tank drive physics
- Compatible with AutoLogic

#### RobotDrive (Real Hardware)

Example real robot implementation:

```java
public class RobotDrive implements DriveIO {
    private DcMotor leftMotor;
    private DcMotor rightMotor;
    private Servo shooter;
    private Servo collector;

    public RobotDrive(HardwareMap hwMap) {
        leftMotor = hwMap.get(DcMotor.class, "left_motor");
        rightMotor = hwMap.get(DcMotor.class, "right_motor");
        shooter = hwMap.get(Servo.class, "shooter");
        collector = hwMap.get(Servo.class, "collector");
    }

    @Override
    public void tank(double left, double right) {
        leftMotor.setPower(left);
        rightMotor.setPower(right);
    }

    @Override
    public void driveStraight(double inches, double power) {
        // Use encoders to drive specific distance
        int target = (int)(inches * TICKS_PER_INCH);
        leftMotor.setTargetPosition(target);
        rightMotor.setTargetPosition(target);
        leftMotor.setPower(power);
        rightMotor.setPower(power);
    }

    @Override
    public void rotate(double degrees, double power) {
        // Use gyro to rotate specific angle
        int target = (int)(degrees * TICKS_PER_DEGREE);
        // ... rotate logic
    }

    @Override
    public void shoot(int count) {
        // Activate shooter mechanism
        shooter.setPosition(1.0);
        // Wait for shooting
        shooter.setPosition(0.0);
    }

    @Override
    public void collect(int batch) {
        // Activate collector mechanism
        collector.setPosition(1.0);
        // Wait for collection
        collector.setPosition(0.0);
    }
}
```

## AprilTagIO Interface

### Purpose

`AprilTagIO` defines the contract for AprilTag detection, allowing both simulated and real vision implementations.

### Location

`@/s:\FTC\simulation\AprilTagIO.java:1-6`

### Interface Definition

```java
public interface AprilTagIO {
    boolean isVisible();
    double yaw();    // radians
    double range();  // inches
}
```

### Methods

#### `isVisible()`

Check if tag is currently visible.

**Returns**: `true` if visible, `false` otherwise

**Usage**:
```java
if (tag.isVisible()) {
    double yaw = tag.yaw();
    double range = tag.range();
}
```

#### `yaw()`

Get yaw angle relative to robot.

**Returns**: Yaw angle in radians

**Usage**:
```java
double yawRad = tag.yaw();
double yawDeg = Math.toDegrees(yawRad);
```

#### `range()`

Get distance from robot to tag.

**Returns**: Range in inches

**Usage**:
```java
double range = tag.range();
```

### Implementations

#### SimAprilTag

Simulated AprilTag with realistic camera constraints.

```java
public class SimAprilTag implements AprilTagIO {
    // ... implementation
}
```

**Features**:
- Realistic FOV constraints
- Range limits
- Orientation tracking
- Yaw bias simulation

#### RobotAprilTag (Real Hardware)

Example real robot implementation:

```java
public class RobotAprilTag implements AprilTagIO {
    private AprilTagDetection detection;

    public RobotAprilTag(AprilTagProcessor processor) {
        detection = processor.getDetection();
    }

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

## Usage with AutoLogic

The interfaces enable AutoLogic to work with both simulation and real hardware:

```java
// Simulation
AutoLogic autoLogic = new AutoLogic();
SimDrive simDrive = new SimDrive();
SimAprilTag simTag21 = new SimAprilTag(5, 0.0, 72.0);
SimAprilTag simTag24 = new SimAprilTag(6, 60.5, 62.0, Math.toRadians(-30));

autoLogic.update(0.02, simTag21, simTag24, simDrive);

// Real Robot (same AutoLogic)
AutoLogic autoLogic = new AutoLogic();
RobotDrive robotDrive = new RobotDrive(hwMap);
RobotAprilTag robotTag21 = new RobotAprilTag(tagProcessor21);
RobotAprilTag robotTag24 = new RobotAprilTag(tagProcessor24);

autoLogic.update(0.02, robotTag21, robotTag24, robotDrive);
```

## Benefits

1. **Separation of Concerns**: Logic separated from hardware
2. **Testing**: Test logic in simulation without robot
3. **Portability**: Same code works on sim and real robot
4. **Flexibility**: Easy to swap implementations
5. **Mocking**: Can create mock implementations for unit tests

## Creating Custom Implementations

### Custom Drive Implementation

```java
public class MyCustomDrive implements DriveIO {
    @Override
    public void tank(double left, double right) {
        // Your implementation
    }

    @Override
    public void driveStraight(double inches, double power) {
        // Your implementation
    }

    @Override
    public void rotate(double degrees, double power) {
        // Your implementation
    }

    @Override
    public void shoot(int count) {
        // Your implementation
    }

    @Override
    public void collect(int batch) {
        // Your implementation
    }
}
```

### Custom AprilTag Implementation

```java
public class MyCustomAprilTag implements AprilTagIO {
    private boolean visible;
    private double yaw;
    private double range;

    public MyCustomAprilTag(boolean visible, double yaw, double range) {
        this.visible = visible;
        this.yaw = yaw;
        this.range = range;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public double yaw() {
        return yaw;
    }

    @Override
    public double range() {
        return range;
    }
}
```

## Notes

- Interfaces use standard Java conventions
- No external dependencies required
- Compatible with FTC SDK
- Thread-safe for single-threaded use
- Designed for 50Hz update rate
