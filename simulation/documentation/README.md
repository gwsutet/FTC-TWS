# FTC Autonomous Simulator

## Overview

This is a Java-based simulator for FTC (FIRST Tech Challenge) autonomous robot programming. It provides a visual simulation environment for testing and developing autonomous logic without needing physical hardware.

## Project Structure

```
simulation/
├── src/                        # Java source files
│   ├── Main.java
│   ├── SimulatorWindow.java
│   ├── AutoLogic.java
│   ├── SimDrive.java
│   ├── SimAprilTag.java
│   ├── DriveIO.java
│   ├── AprilTagIO.java
│   └── SavedPosition.java
├── bin/                        # Compiled class files
├── documentation/              # Full documentation
│   ├── README.md              # Project overview
│   ├── QUICK_START.md         # 5-minute setup
│   ├── USAGE.md               # Detailed usage guide
│   ├── AUTOLOGIC.md           # State machine docs
│   ├── SIMDRIVE.md            # Drive system docs
│   ├── SIMAPRILTAG.md         # AprilTag docs
│   ├── SIMULATORWINDOW.md     # GUI docs
│   ├── INTERFACES.md          # Interface docs
│   └── AUTO_VARIANTS.md       # Variant strategies
├── auto_logic_variants/        # Auto strategy variants
│   ├── BLUE/
│   └── RED/
├── field/                      # Field images
└── saved_positions.dat         # Saved positions
```

## Quick Start

```bash
# Compile
javac -d bin -sourcepath src src/*.java

# Run
java -cp bin Main
```

## Features

- **Visual Field Simulation**: 144" x 144" field with customizable background image
- **AprilTag Detection**: Simulated AprilTag with realistic camera constraints (FOV, range limits)
- **State Machine Logic**: Modular autonomous logic with configurable states
- **Position Tracking**: Real-time pose tracking (x, y, heading)
- **Trail Visualization**: Shows robot path history
- **Saved Positions**: Save and load robot positions for testing
- **Multiple Auto Variants**: Different autonomous strategies for RED and BLUE alliances

## Quick Start

### Running the Simulator

```bash
# Compile and run
javac *.java
java Main
```

### Basic Controls

- **ENTER**: Toggle autonomous mode on/off
- **1**: Toggle visibility of Tag21 (center tag)
- **2**: Toggle visibility of Tag24 (shooter tag)
- **Q/E**: Adjust yaw of currently selected tag
- **W/S**: Adjust range of currently selected tag
- **Set Pose**: Set robot starting position
- **Reset Trail**: Clear robot path visualization

### GUI Controls

- **Start X/Y/Heading**: Set robot initial position
- **Set Pose Button**: Apply the position settings
- **Reset Trail**: Clear the robot's path
- **Load Field Image**: Load a custom field background
- **Saved Positions**: Save, load, or delete robot positions

## Coordinate System

- **Origin**: Field center (0, 0)
- **+X**: Audience right
- **+Y**: Away from audience
- **Heading**: 0° = facing +Y, positive = clockwise
- **Field Size**: 144" x 144"

## AprilTag Layout

Current configuration (Centerstage style):
- **Tag4**: (-60.5, 62.0) with 45° rotation (red)
- **Tag5**: (0.0, 72.0) (purple)
- **Tag6**: (60.5, 62.0) with -30° rotation (yellow)

Legacy aliases for auto logic:
- **Tag21**: Maps to Tag5 (center)
- **Tag24**: Maps to Tag6 (shooter)

## Camera Simulation

Simulates Logitech C270 webcam:
- **FOV**: 55° horizontal
- **Max Range**: 120 inches
- **Camera Height**: 16 inches
- **Tag Height**: 23 inches (pylon top)
- **Max Yaw**: ±27.5° (half FOV)
- **Max Pitch**: 20° upward

## Auto Logic State Machine

The autonomous logic uses a state machine with these states:

1. **IDLE**: Initial state, starts sequence
2. **SCAN_CENTER**: Search and align with center tag (Tag21)
3. **SEARCH_SHOOTER**: Search for shooter tag (Tag24)
4. **LOCK_SHOOTER**: Align precisely with shooter tag
5. **TURN_BACK**: Rotate 180° to face away
6. **SHOOTING**: Shoot balls
7. **COLLECT_BALLS**: Collect more balls and repeat

See [AUTOLOGIC.md](AUTOLOGIC.md) for detailed state documentation.

## Development

### Adding New Auto Variants

1. Copy existing AutoLogic.java to `auto_logic_variants/<ALLIANCE>/<NAME>.java`
2. Modify the state machine logic
3. Adjust parameters (speeds, distances, tolerances)
4. Test in simulator before deploying to robot

### Modifying Drive Parameters

Edit `SimDrive.java`:
- `maxLinearSpeed`: Maximum inches per second
- `trackWidth`: Distance between wheels (inches)

### Adding New AprilTags

Edit `SimulatorWindow.java`:
```java
private final SimAprilTag tag7 = new SimAprilTag(7, x, y, orientation);
```

## File Descriptions

- **Main.java**: Entry point, creates and shows SimulatorWindow
- **SimulatorWindow.java**: Main GUI, handles rendering, controls, and main loop
- **AutoLogic.java**: Base autonomous state machine implementation
- **SimDrive.java**: Simulated differential drive with pose tracking
- **SimAprilTag.java**: AprilTag simulation with realistic camera constraints
- **DriveIO.java**: Interface for drive operations (tank, driveStraight, rotate, shoot, collect)
- **AprilTagIO.java**: Interface for AprilTag readings (isVisible, yaw, range)
- **SavedPosition.java**: Serializable class for saving robot positions

## Dependencies

- Java 8 or higher
- Swing (included in JDK)
- No external libraries required

## Documentation

- [AUTOLOGIC.md](AUTOLOGIC.md) - AutoLogic state machine documentation
- [SIMDRIVE.md](SIMDRIVE.md) - SimDrive system documentation
- [SIMAPRILTAG.md](SIMAPRILTAG.md) - AprilTag simulation documentation
- [USAGE.md](USAGE.md) - Detailed usage guide with examples
- [SIMULATORWINDOW.md](SIMULATORWINDOW.md) - GUI and controls documentation

## License

This is a simulation tool for FTC autonomous development.
