# Usage Guide

## Running the Simulator

### Compilation

```bash
cd s:\FTC\simulation
javac -d bin -sourcepath src src/*.java
```

### Execution

```bash
java -cp bin Main
```

The simulator window will appear with:
- Field view in center
- Control panel at top
- Info panel on right

## Keyboard Controls

### Autonomous Mode

| Key | Action |
|-----|--------|
| ENTER | Toggle autonomous mode ON/OFF |

When auto is enabled:
- Robot follows autonomous sequence
- State transitions displayed in info panel
- Trail shows robot path

When auto is disabled:
- Robot stops immediately
- Manual control available

### AprilTag Controls

| Key | Action |
|-----|--------|
| 1 | Toggle Tag21 visibility (center tag) |
| 2 | Toggle Tag24 visibility (shooter tag) |
| Q | Decrease yaw of selected tag by 0.05 rad |
| E | Increase yaw of selected tag by 0.05 rad |
| W | Decrease range of selected tag by 2 inches |
| S | Increase range of selected tag by 2 inches |

**Note**: After pressing 1 or 2, that tag becomes the "control tag" for Q/E/W/S adjustments.

## GUI Controls

### Start Position Controls

**Fields**:
- **Start X (in)**: Initial X coordinate
- **Start Y (in)**: Initial Y coordinate
- **Heading (deg)**: Initial heading (0° = facing +Y)

**Buttons**:
- **Set Pose**: Apply the position values to robot
- **Reset Trail**: Clear the robot's path visualization
- **Load Field Image**: Load custom field background

### Saved Positions

**Dropdown**: Select saved position

**Buttons**:
- **Save**: Save current position (prompts for name)
- **Load**: Load selected position
- **Delete**: Delete selected position

## Coordinate System

```
         +Y (away from audience)
         |
         |
         |
---------+--------- +X (audience right)
         |
         |
         |
         -Y (toward audience)
```

- Origin (0,0) = field center
- +X = right from audience perspective
- +Y = away from audience
- Heading: 0° = facing +Y, positive = clockwise

## Typical Workflow

### 1. Set Starting Position

```
Start X: 15
Start Y: -55
Heading: 45
```

Click **Set Pose**

### 2. Configure AprilTags

Press **1** to enable Tag21 (center)
Press **2** to enable Tag24 (shooter)

Optional: Adjust with Q/E/W/S for testing

### 3. Start Autonomous

Press **ENTER** to enable auto

Watch:
- State transitions in info panel
- Robot movement on field
- Trail showing path

Press **ENTER** again to stop

### 4. Reset and Retry

Click **Reset Trail** to clear path
Click **Set Pose** to reset position
Press **ENTER** to restart

## Testing Different Scenarios

### Scenario 1: Test Tag Alignment

1. Set position: X=15, Y=-55, Heading=45
2. Enable Tag21 (press 1)
3. Enable auto (ENTER)
4. Watch SCAN_CENTER state
5. Observe yaw alignment behavior

### Scenario 2: Test Shooting Sequence

1. Set position: X=15, Y=-55, Heading=45
2. Enable both tags (1 and 2)
3. Enable auto (ENTER)
4. Watch full sequence:
   - SCAN_CENTER
   - SEARCH_SHOOTER
   - LOCK_SHOOTER
   - TURN_BACK
   - SHOOTING
   - COLLECT_BALLS

### Scenario 3: Test Tag Loss Recovery

1. Set position: X=15, Y=-55, Heading=45
2. Enable Tag21 only (press 1)
3. Enable auto (ENTER)
4. When in SEARCH_SHOOTER, disable Tag24 (press 2)
5. Watch robot search for tag
6. Re-enable Tag24 (press 2)
7. Watch robot lock on and continue

### Scenario 4: Test Position Saving

1. Move robot to desired position
2. Click **Save**
3. Enter name: "Test Position"
4. Position saved to dropdown
5. Move robot elsewhere
6. Select "Test Position" from dropdown
7. Click **Load**
8. Robot teleports to saved position

## Debugging Tips

### Check State Transitions

Look at "Recent state transitions" in info panel:
```
IDLE -> SCAN_CENTER
SCAN_CENTER -> SEARCH_SHOOTER
SEARCH_SHOOTER -> LOCK_SHOOTER
```

### Monitor Tag Visibility

HUD shows:
```
Tag21 vis=true yaw=0.12 rad r=60.5
Tag24 vis=true yaw=-0.05 rad r=68.0
```

### Track Robot Pose

HUD shows:
```
Pose: x=15.2 in, y=-54.8 in, heading=45.3 deg
```

### View Trail

Blue line shows robot path history (last 2000 points)

## Custom Field Background

1. Prepare field image (PNG/JPG)
2. Click **Load Field Image**
3. Select image file
4. Field displays in background

**Recommended**: 144" x 144" field image

## Saved Positions File

Positions saved to `saved_positions.dat`

Format: Serialized `List<SavedPosition>`

To clear:
1. Delete `saved_positions.dat`
2. Restart simulator

## Performance Tips

- Trail limited to 2000 points for performance
- Update rate: 50Hz (20ms)
- Field scale auto-calculated to fit window

## Common Issues

### Issue: Auto doesn't start

**Solution**: Ensure tags are visible (press 1 and 2)

### Issue: Robot spins endlessly

**Solution**: Tags not visible or out of range. Check:
- Tag visibility in HUD
- Robot position relative to tags
- Camera FOV constraints

### Issue: Robot doesn't move

**Solution**:
- Check auto is enabled (ENTER)
- Check current state in HUD
- Verify drive system initialized

### Issue: Trail not clearing

**Solution**: Click **Reset Trail** button

## Advanced Usage

### Modify Auto Parameters

Edit `AutoLogic.java`:

```java
// Change target range
double rangeError = tag21.range() - 60.0; // Change 60.0

// Change tolerance
private final double distanceTolerance = 1.0; // Change 1.0

// Change speeds
private final double linearSpeedInPerSec = 100.0;
```

### Add Custom States

1. Add state to enum
2. Add case in switch statement
3. Implement logic
4. Add transitions

### Create Auto Variants

Copy existing logic to variant folder:
```
auto_logic_variants/RED/MY_AUTO.java
auto_logic_variants/BLUE/MY_AUTO.java
```

Modify parameters and logic as needed.

## Command Line Examples

### Compile and Run

```bash
# Windows
javac *.java
java Main

# With classpath
javac -cp . *.java
java -cp . Main
```

### Clean Build

```bash
del *.class
javac *.java
java Main
```

### Compile Specific File

```bash
javac AutoLogic.java
```

## Tips

1. **Start Simple**: Test one state at a time
2. **Use Trail**: Visualize path to debug
3. **Save Positions**: Save good starting points
4. **Monitor HUD**: Real-time feedback
5. **Reset Often**: Clear trail between tests
6. **Toggle Tags**: Test with/without tags
7. **Adjust Parameters**: Fine-tune distances/speeds

## Integration with Real Robot

To use on real robot:

1. Replace `SimDrive` with real drive implementation
2. Replace `SimAprilTag` with real AprilTag vision
3. Keep `AutoLogic` unchanged (uses interfaces)
4. Deploy to robot controller

The simulator uses the same interfaces as real hardware, making transition seamless.
