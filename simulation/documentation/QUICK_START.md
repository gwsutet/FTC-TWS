# Quick Start Guide

## 5-Minute Setup

### 1. Compile and Run

```bash
cd s:\FTC\simulation
javac -d bin -sourcepath src src/*.java
java -cp bin Main
```

### 2. Set Starting Position

Enter in control panel:
- **Start X**: 15
- **Start Y**: -55
- **Heading**: 45

Click **Set Pose**

### 3. Enable Tags

Press:
- **1** - Enable Tag21 (center)
- **2** - Enable Tag24 (shooter)

### 4. Run Autonomous

Press **ENTER** to enable auto

Watch the robot:
- Scan for center tag
- Align with shooter tag
- Turn and shoot
- Collect balls
- Repeat

Press **ENTER** again to stop

## Essential Controls

| Key | Action |
|-----|--------|
| ENTER | Toggle auto ON/OFF |
| 1 | Toggle Tag21 visibility |
| 2 | Toggle Tag24 visibility |
| Q | Decrease tag yaw |
| E | Increase tag yaw |
| W | Decrease tag range |
| S | Increase tag range |

## GUI Buttons

- **Set Pose**: Apply position settings
- **Reset Trail**: Clear robot path
- **Load Field Image**: Load custom background
- **Save**: Save current position
- **Load**: Load saved position
- **Delete**: Delete saved position

## Reading the HUD

```
Auto: ENABLED              // Auto mode status
State: SCAN_CENTER         // Current state
Pose: x=15.2 y=-54.8 h=45.3  // Robot position
Tag21 vis=true yaw=0.12 r=60.5  // Tag21 info
Tag24 vis=true yaw=-0.05 r=68.0 // Tag24 info
```

## State Machine Flow

```
IDLE → SCAN_CENTER → SEARCH_SHOOTER → LOCK_SHOOTER
     ↓
TURN_BACK → SHOOTING → COLLECT_BALLS → SEARCH_SHOOTER (loop)
```

## Common Tasks

### Test Tag Alignment

1. Set position: X=15, Y=-55, Heading=45
2. Enable Tag21 (press 1)
3. Enable auto (ENTER)
4. Watch SCAN_CENTER state

### Test Full Sequence

1. Set position: X=15, Y=-55, Heading=45
2. Enable both tags (1 and 2)
3. Enable auto (ENTER)
4. Watch full autonomous sequence

### Save a Position

1. Move robot to desired position
2. Click **Save**
3. Enter name: "My Start"
4. Position saved

### Load a Position

1. Select position from dropdown
2. Click **Load**
3. Robot teleports to position

## Troubleshooting

**Auto doesn't start?**
- Ensure tags are visible (press 1 and 2)

**Robot spins endlessly?**
- Tags not visible or out of range. Check HUD for visibility.

**Robot doesn't move?**
- Check auto is enabled (ENTER)
- Check current state in HUD

**Trail not clearing?**
- Click **Reset Trail** button

## Next Steps

- Read [USAGE.md](USAGE.md) for detailed guide
- Read [AUTOLOGIC.md](AUTOLOGIC.md) for state machine details
- Read [SIMDRIVE.md](SIMDRIVE.md) for drive system
- Read [SIMAPRILTAG.md](SIMAPRILTAG.md) for AprilTag system

## Need Help?

Check documentation:
- [README.md](README.md) - Project overview
- [USAGE.md](USAGE.md) - Detailed usage guide
- [AUTOLOGIC.md](AUTOLOGIC.md) - State machine documentation
- [INTERFACES.md](INTERFACES.md) - Interface documentation
