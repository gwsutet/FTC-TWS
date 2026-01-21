# Auto Logic Variants Documentation

## Overview

The `auto_logic_variants` folder contains different autonomous strategies for different alliances and scenarios. Each variant is a modified version of the base `AutoLogic` class with customized parameters and behaviors.

## Folder Structure

```
auto_logic_variants/
├── BLUE/
│   └── BLUE_AUTO_FAR.java
└── RED/
    ├── RED_AUTO_FAR.java
    ├── RED_SAMPING1.java
    ├── RED_SAMPING2.java
    └── converted/
        └── RED_AUTO_FAR.JAVA
```

## Variant Types

### RED Variants

#### RED_AUTO_FAR.java

**Purpose**: RED alliance autonomous strategy for far starting position

**Key Features**:
- Starting position: X=5.0, Y=-55.0, Heading=45.0
- Targets Tag21 (center) and Tag24 (shooter)
- Standard state machine flow

**Differences from Base**:
- Modified initial sequence
- Adjusted distances for RED alliance
- Custom collection sequence

**Usage**:
```java
AutoLogic autoLogic = new AutoLogic();
// Use RED_AUTO_FAR.java logic
```

#### RED_SAMPING1.java

**Purpose**: RED alliance sampling strategy variant 1

**Key Features**:
- Alternative sampling positions
- Different tag priorities
- Modified search patterns

**Differences from Base**:
- Different initial sequence
- Alternative SEARCH_SHOOTER logic
- Custom COLLECT_BALLS behavior

#### RED_SAMPING2.java

**Purpose**: RED alliance sampling strategy variant 2

**Key Features**:
- Second sampling approach
- Different distance tolerances
- Alternative rotation angles

**Differences from Base**:
- Modified distance tolerances
- Different rotation angles
- Custom power settings

#### converted/RED_AUTO_FAR.JAVA

**Purpose**: Converted version of RED_AUTO_FAR (legacy)

**Status**: Legacy file, may be outdated

### BLUE Variants

#### BLUE_AUTO_FAR.java

**Purpose**: BLUE alliance autonomous strategy for far starting position

**Key Features**:
- Starting position: Mirrored from RED
- Targets same tags but from opposite side
- Mirrored state machine flow

**Differences from Base**:
- Mirrored initial position
- Adjusted rotation directions
- Inverted distance signs

## Creating New Variants

### Step 1: Copy Base AutoLogic

```bash
# Create new variant
cp AutoLogic.java auto_logic_variants/RED/MY_VARIANT.java
```

### Step 2: Modify Initial Position

```java
// Set initial position for your variant
private double initialX = 15.0;   // Adjust for your start position
private double initialY = -55.0;  // Adjust for your start position
private double initialHeading = 45.0; // Adjust for your heading
```

### Step 3: Customize State Machine

Modify states as needed:

```java
// Example: Modify SCAN_CENTER state
case SCAN_CENTER:
    if (tag21 != null && tag21.isVisible()) {
        double yawError = tag21.yaw();
        if (Math.abs(yawError) > Math.toRadians(5)) {
            drive.tank(-0.25, 0.25);  // Adjust power
        } else {
            drive.tank(0, 0);
            double rangeError = tag21.range() - 60.0;  // Adjust target range
            if (Math.abs(rangeError) > distanceTolerance) {
                startDriveAction(rangeError, 0.9, drive, null);
            } else {
                currentState = AutoState.SEARCH_SHOOTER;
            }
        }
    } else {
        drive.tank(-0.18, 0.18);  // Adjust search speed
    }
    break;
```

### Step 4: Adjust Parameters

```java
// Modify speeds
private final double linearSpeedInPerSec = 100.0;  // Adjust max speed
private final double angularSpeedDegPerSec = 360.0; // Adjust rotation speed
private final double distanceTolerance = 1.0;     // Adjust tolerance

// Modify initial sequence
private void startInitialSequence(DriveIO drive) {
    // Customize your sequence
    startDriveAction(-80.0, 0.9, drive, 
     () -> startRotateAction(-90.0, 1.0, drive, 
        () -> startDriveAction(-90.0, 0.9, drive, 
            () -> currentState = AutoState.SCAN_CENTER)));
}

// Modify collection sequence
private void startCollectSequence(DriveIO drive) {
    ballBatchCount++;
    double driveDist = 55.0;  // Adjust distance
    // Customize your collection sequence
}
```

### Step 5: Test in Simulator

1. Compile variant
2. Run simulator
3. Set start position
4. Enable auto
5. Observe behavior
6. Adjust as needed

## Common Modifications

### Adjust Starting Position

```java
// Change initial position
private double initialX = 20.0;   // Different X
private double initialY = -60.0;  // Different Y
private double initialHeading = 0.0; // Different heading
```

### Modify Target Ranges

```java
// In SCAN_CENTER state
double rangeError = tag21.range() - 60.0;  // Change 60.0

// In LOCK_SHOOTER state
double rangeError = tag24.range() - 68.0;  // Change 68.0
```

### Change Tolerances

```java
// Yaw tolerance
if (Math.abs(yawError) > Math.toRadians(5)) {  // Change 5

// Distance tolerance
private final double distanceTolerance = 1.0;  // Change 1.0
```

### Adjust Speeds

```java
// Linear speed
private final double linearSpeedInPerSec = 100.0;  // Change 100.0

// Angular speed
private final double angularSpeedDegPerSec = 360.0; // Change 360.0

// Motor power
drive.tank(-0.25, 0.25);  // Change 0.25
```

### Modify Sequence

```java
// Change initial sequence
private void startInitialSequence(DriveIO drive) {
    // Your custom sequence
    startDriveAction(-50.0, 0.9, drive, 
     () -> startRotateAction(-45.0, 1.0, drive, 
        () -> currentState = AutoState.SCAN_CENTER));
}

// Change collection sequence
private void startCollectSequence(DriveIO drive) {
    ballBatchCount++;
    double driveDist = 60.0;  // Change distance
    // Your custom collection logic
}
```

## Testing Variants

### Test Procedure

1. **Compile Variant**
```bash
javac auto_logic_variants/RED/MY_VARIANT.java
```

2. **Run Simulator**
```bash
java Main
```

3. **Set Start Position**
- Enter X, Y, Heading
- Click "Set Pose"

4. **Enable Tags**
- Press 1 for Tag21
- Press 2 for Tag24

5. **Run Auto**
- Press ENTER to enable
- Watch state transitions
- Observe robot movement

6. **Analyze Results**
- Check state transitions in HUD
- Verify robot reaches targets
- Check alignment accuracy
- Note any issues

7. **Iterate**
- Adjust parameters
- Retest
- Repeat until satisfied

### Debug Tips

**Issue**: Robot doesn't find tags

**Solutions**:
- Check tag visibility (press 1, 2)
- Verify robot position relative to tags
- Adjust search speed
- Check FOV constraints

**Issue**: Robot overshoots targets

**Solutions**:
- Reduce motor power
- Increase distance tolerance
- Adjust linear speed
- Add deceleration

**Issue**: Robot oscillates

**Solutions**:
- Increase yaw tolerance
- Reduce alignment power
- Add damping logic
- Check for sensor noise

## Comparison Table

| Variant | Alliance | Start X | Start Y | Heading | Key Differences |
|---------|----------|---------|---------|---------|-----------------|
| RED_AUTO_FAR | RED | 5.0 | -55.0 | 45.0 | Standard RED strategy |
| RED_SAMPING1 | RED | 5.0 | -55.0 | 45.0 | Alternative sampling |
| RED_SAMPING2 | RED | 5.0 | -55.0 | 45.0 | Second sampling approach |
| BLUE_AUTO_FAR | BLUE | -5.0 | -55.0 | -45.0 | Mirrored RED strategy |

## Best Practices

1. **Start Simple**: Begin with base AutoLogic, make small changes
2. **Test Often**: Run simulator after each modification
3. **Document Changes**: Comment on what and why you changed
4. **Keep Backups**: Save working versions before major changes
5. **Use Version Control**: Track changes with git
6. **Name Clearly**: Use descriptive variant names
7. **Test Edge Cases**: Check what happens when tags are lost
8. **Monitor Performance**: Ensure updates complete within 20ms

## Integration with Simulator

To use a variant in the simulator:

1. **Copy variant to main directory**
```bash
cp auto_logic_variants/RED/MY_VARIANT.java AutoLogic.java
```

2. **Recompile**
```bash
javac *.java
```

3. **Run**
```bash
java Main
```

Or modify `SimulatorWindow.java` to use variant directly:

```java
// In SimulatorWindow.java
// import your variant
import RED_AUTO_FAR;

// Replace AutoLogic instantiation
private final AutoLogic autoLogic = new RED_AUTO_FAR();
```

## Notes

- Variants inherit from base AutoLogic structure
- Can add new states if needed
- Keep interfaces compatible
- Test thoroughly before competition
- Document all changes for team reference
