# FTC Autonomous Simulator

## Quick Start

```bash
# Compile
javac -d bin -sourcepath src src/*.java

# Run
java -cp bin Main
```

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

## Documentation

See [documentation/](documentation/) for complete documentation:
- Start with [QUICK_START.md](documentation/QUICK_START.md)
- Read [README.md](documentation/README.md) for full overview
- Check [USAGE.md](documentation/USAGE.md) for detailed commands

## Controls

| Key | Action |
|-----|--------|
| ENTER | Toggle autonomous mode |
| 1 | Toggle Tag21 visibility |
| 2 | Toggle Tag24 visibility |
| Q/E | Adjust tag yaw |
| W/S | Adjust tag range |

## Features

- Visual field simulation (144" x 144")
- AprilTag detection with realistic camera constraints
- State machine autonomous logic
- Real-time pose tracking
- Position saving/loading
- Multiple auto variants (RED/BLUE)

## Build & Run

```bash
# Clean build
del bin\*.class
javac -d bin -sourcepath src src\*.java

# Run
java -cp bin Main
```

## Requirements

- Java 8 or higher
- No external dependencies
