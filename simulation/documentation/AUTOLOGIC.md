# AutoLogic Documentation

## Overview

`AutoLogic` is the core autonomous logic class that implements a state machine for robot navigation and task execution. It uses a finite state machine (FSM) pattern to manage autonomous sequences.

## Class Structure

```java
public class AutoLogic {
    public enum AutoState {
        IDLE, SCAN_CENTER, SEARCH_SHOOTER, 
        LOCK_SHOOTER, TURN_BACK, SHOOTING, COLLECT_BALLS
    }
}
```

## State Machine

### 1. IDLE
**Purpose**: Initial state, starts the autonomous sequence

**Behavior**:
- Sets robot to initial position (if configured)
- Starts the initial movement sequence
- Transitions to `SCAN_CENTER` after initial sequence completes

**Initial Sequence** (default):
- Drive -80 inches
- Rotate -90°
- Drive -90 inches
- → SCAN_CENTER

### 2. SCAN_CENTER
**Purpose**: Locate and align with the center tag (Tag21)

**Behavior**:
- If Tag21 visible:
  - Align yaw to face tag (±5° tolerance)
  - Drive to 60 inches from tag
  - → SEARCH_SHOOTER
- If Tag21 not visible:
  - Rotate counter-clockwise to search
  - Tank drive: left=-0.18, right=0.18

**Parameters**:
- Yaw tolerance: 5°
- Target range: 60 inches
- Search rotation speed: 0.18 power

### 3. SEARCH_SHOOTER
**Purpose**: Locate the shooter tag (Tag24)

**Behavior**:
- If Tag24 visible:
  - Align yaw to face tag (±5° tolerance)
  - → LOCK_SHOOTER
- If Tag24 not visible:
  - Rotate clockwise to search
  - Tank drive: left=0.18, right=-0.18

**Parameters**:
- Yaw tolerance: 5°
- Search rotation speed: 0.18 power

### 4. LOCK_SHOOTER
**Purpose**: Precisely align with shooter tag for shooting position

**Behavior**:
- If Tag24 visible:
  - Fine-tune yaw alignment (±3° tolerance)
  - Drive to 68 inches from tag
  - → TURN_BACK
- If Tag24 lost:
  - → SEARCH_SHOOTER

**Parameters**:
- Yaw tolerance: 3°
- Target range: 68 inches
- Alignment power: 0.25

### 5. TURN_BACK
**Purpose**: Rotate 180° to face away from shooter tag

**Behavior**:
- Rotate -180° (counter-clockwise)
- → SHOOTING

**Parameters**:
- Rotation: -180°
- Power: 0.9

### 6. SHOOTING
**Purpose**: Shoot balls into target

**Behavior**:
- Call `drive.shoot(3)` (simulated)
- Wait 2 seconds
- → COLLECT_BALLS

**Parameters**:
- Ball count: 3
- Duration: 2 seconds

### 7. COLLECT_BALLS
**Purpose**: Collect more balls and return to shooting position

**Behavior**:
- Increment ball batch counter
- Execute collection sequence:
  1. Rotate -79.2°
  2. Drive 55 inches
  3. Rotate -180°
  4. Drive -120 inches
  5. Rotate -180°
  6. Collect balls
  7. Drive 50 inches (fast)
  8. Drive 70 inches (slow)
  9. Sleep 1 second
  10. Drive -120 inches
  11. Rotate -180°
  12. Drive 55 inches
  13. Rotate 45°
  14. → SEARCH_SHOOTER

**Parameters**:
- Drive distance: 55 inches per batch
- Fast drive: 50 inches at 0.8 power
- Slow drive: 70 inches at 0.3 power
- Sleep duration: 1 second
- Rotation angles: -79.2°, -180°, 45°

## Key Methods

### `update(double dt, AprilTagIO tag21, AprilTagIO tag24, DriveIO drive)`

Main update loop called every frame (50Hz).

**Parameters**:
- `dt`: Delta time in seconds
- `tag21`: Center AprilTag reference
- `tag24`: Shooter AprilTag reference
- `drive`: Drive system interface

**Behavior**:
- Handles action timers
- Handles sleep timers
- Executes current state logic
- Manages state transitions

### `reset()`

Resets the autonomous logic to initial state.

**Behavior**:
- Sets state to IDLE
- Resets ball batch counter
- Clears all timers and callbacks
- Resets started flag

### `setInitialPosition(double x, double y, double heading)`

Sets the robot's starting position for simulation.

**Parameters**:
- `x`: X coordinate (inches)
- `y`: Y coordinate (inches)
- `heading`: Heading (degrees)

### `setUseInitialPosition(boolean use)`

Enables or disables initial position setting.

## Action Helpers

### `startDriveAction(double inches, double power, DriveIO drive, Runnable onDone)`

Executes a straight drive action.

**Parameters**:
- `inches`: Distance to drive (negative = backward)
- `power`: Motor power (-1.0 to 1.0)
- `drive`: Drive system
- `onDone`: Callback when complete

**Behavior**:
- Calculates duration based on speed
- Calls `drive.driveStraight()`
- Sets timer for completion

### `startRotateAction(double degrees, double power, DriveIO drive, Runnable onDone)`

Executes a rotation action.

**Parameters**:
- `degrees`: Degrees to rotate (negative = counter-clockwise)
- `power`: Motor power (-1.0 to 1.0)
- `drive`: Drive system
- `onDone`: Callback when complete

### `startShootAction(DriveIO drive, Runnable onDone)`

Executes shooting action.

**Parameters**:
- `drive`: Drive system
- `onDone`: Callback when complete

### `beginSleep(double durationSeconds, Runnable onDone)`

Pauses execution for specified duration.

**Parameters**:
- `durationSeconds`: Time to sleep
- `onDone`: Callback when complete

**Behavior**:
- Stops all movement during sleep
- Calls callback after timer expires

## Configuration Parameters

```java
private final double linearSpeedInPerSec = 100.0;  // Max linear speed
private final double angularSpeedDegPerSec = 360.0; // Max angular speed
private final double distanceTolerance = 1.0;     // Distance tolerance (inches)
```

## Initial Position Settings

```java
private double initialX = 5.0;       // Default starting X
private double initialY = -55.0;    // Default starting Y
private double initialHeading = 45.0; // Default heading
private boolean useInitialPosition = false; // Enable/disable
```

## State Transitions

```
IDLE → SCAN_CENTER (after initial sequence)
SCAN_CENTER → SEARCH_SHOOTER (when aligned with Tag21)
SEARCH_SHOOTER → LOCK_SHOOTER (when Tag24 found)
LOCK_SHOOTER → TURN_BACK (when aligned and at range)
LOCK_SHOOTER → SEARCH_SHOOTER (if Tag24 lost)
TURN_BACK → SHOOTING (after rotation)
SHOOTING → COLLECT_BALLS (after shooting)
COLLECT_BALLS → SEARCH_SHOOTER (after collection sequence)
```

## Usage Example

```java
AutoLogic autoLogic = new AutoLogic();

// Set initial position
autoLogic.setInitialPosition(15.0, -55.0, 45.0);
autoLogic.setUseInitialPosition(true);

// Main loop
while (running) {
    autoLogic.update(0.02, tag21, tag24, drive);
    // ... other updates
}

// Reset when needed
autoLogic.reset();
```

## Customizing AutoLogic

To create a custom autonomous variant:

1. Copy `AutoLogic.java` to your variant folder
2. Modify state machine logic
3. Adjust parameters (distances, speeds, tolerances)
4. Add or remove states as needed

### Example: Adding a new state

```java
public enum AutoState {
    IDLE, SCAN_CENTER, SEARCH_SHOOTER, 
    LOCK_SHOOTER, TURN_BACK, SHOOTING, 
    COLLECT_BALLS, NEW_STATE  // Add new state
}

// Add case in update()
case NEW_STATE:
    // Your logic here
    break;
```

## Debugging

Enable console logging by adding `System.out.println()` statements:

```java
System.out.println("State: " + currentState);
System.out.println("Tag21 visible: " + tag21.isVisible());
System.out.println("Tag24 yaw: " + Math.toDegrees(tag24.yaw()));
```

## Notes

- State machine runs at 50Hz (20ms dt)
- Actions use callbacks for sequencing
- Sleep stops all movement
- Ball batch counter tracks collection cycles
- Tags can be toggled via keyboard (1, 2 keys)
