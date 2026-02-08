# FTC Autonomous Troubleshooting Solutions

## Problem Summary
Your robot spins in circles instead of executing the intended movements (turn -40° then drive straight) in autonomous mode. The teleop mode works correctly.

## Root Cause Analysis

### Primary Issue: Missing While Loop in `moveRobot()`
**File:** `RED_AUTO_FAR.JAVA` (lines 197-212)

The `moveRobot()` function sets up motors for `RUN_TO_POSITION` but **does NOT wait for the motors to reach their target**. It immediately calls `stopDrive()` and switches back to `RUN_USING_ENCODER`, causing the motors to stop before they can move.

**Original buggy code:**
```java
void moveRobot(int leftTicks, int rightTicks, double pwr) {
    // Setup motors...
    leftDrive.setPower(pwr);
    rightDrive.setPower(pwr);
    
    // BUG: No wait loop here!
    
    stopDrive(); // Motors stop immediately!
}
```

**Fixed code (see Fix1):**
```java
void moveRobot(int leftTicks, int rightTicks, double pwr) {
    // Setup motors...
    leftDrive.setPower(pwr);
    rightDrive.setPower(pwr);
    
    // FIX: Wait for motors to complete
    while (opModeIsActive() && (leftDrive.isBusy() || rightDrive.isBusy())) {
        idle();
    }
    
    stopDrive();
}
```

---

## Solution Folders

### 📁 Fix1_Missing_While_Loop
**Use this FIRST - Most likely solution**

Contains `RED_AUTO_FAR_FIX1.java` which adds the missing `while(isBusy())` loop in `moveRobot()`.

**What to check:**
1. Copy this file to your teamcode folder
2. Run on robot
3. Robot should now complete each movement before transitioning states

---

### 📁 Fix2_State_Machine
**Use if Fix1 doesn't fully solve the issue**

Contains `RED_AUTO_FAR_FIX2.java` which adds `stateInitialized` flags to prevent the IDLE state from rapidly re-triggering movements.

**Additional fixes in this version:**
- Added `INIT` and `COMPLETE` states
- Added `stateInitialized` boolean to track if a state's action has started
- Prevents the IDLE state from executing `rotateExactFast()` and `driveStraight()` on every loop iteration

---

### 📁 Fix3_Motor_Direction_Check
**Run this diagnostic FIRST if you're unsure about motor directions**

Contains `Motor_Direction_Test.java` - a standalone diagnostic opmode.

**How to use:**
1. Deploy this file to your robot
2. Run from Driver Station
3. Watch each wheel individually
4. Verify that:
   - Left motor spins FORWARD when commanded
   - Right motor spins FORWARD when commanded
   - Both motors together move the robot FORWARD
   - Turn commands spin the robot in the expected direction

**Note:** Your teleop uses:
```java
leftDrive.setDirection(DcMotor.Direction.FORWARD);
rightDrive.setDirection(DcMotor.Direction.REVERSE);
```
This is correct for standard tank drive.

---

### 📁 Debug_Telemetry_Version
**Use for field debugging if issues persist**

Contains `RED_AUTO_FAR_DEBUG.java` with enhanced telemetry showing:
- Current state
- Motor encoder positions
- `isBusy()` status for both motors
- Motor run modes
- AprilTag detection status

**Diagnostic info displayed:**
```
=== MOTOR STATUS ===
Left Pos: 1234
Right Pos: -1234
Left Busy: true
Right Busy: true
Left Mode: RUN_TO_POSITION
```

---

## Field Testing Checklist

### Step 1: Pre-Flight Checks
- [ ] Verify robot starts in correct orientation
- [ ] Verify all motors respond in teleop mode
- [ ] Check battery voltage (>12V recommended)
- [ ] Verify webcam is connected and streaming

### Step 2: Motor Direction Test
- [ ] Run `Motor_Direction_Test.java`
- [ ] Confirm left wheel spins forward
- [ ] Confirm right wheel spins forward
- [ ] Confirm robot moves forward with both motors
- [ ] If wheels spin backward → swap motor directions in `initHardware()`

### Step 3: Basic Movement Test
- [ ] Deploy `RED_AUTO_FAR_FIX1.java`
- [ ] Place robot at starting position
- [ ] Press INIT then PLAY
- [ ] **Expected behavior:**
  - Robot turns -40° (clockwise/counter-clockwise based on your coordinate system)
  - Robot pauses after turn completes
  - Robot drives straight 40 inches
  - Robot transitions to SCAN_CENTER state

### Step 4: If Still Spinning
- [ ] Deploy `RED_AUTO_FAR_DEBUG.java`
- [ ] Watch telemetry on driver station
- [ ] Check if `isBusy()` shows true/false correctly
- [ ] Check if motor positions are changing
- [ ] Report telemetry readings

---

## Common Issues & Fixes

### Issue: Robot spins rapidly in circles
**Cause:** Missing `while(isBusy())` loop in `moveRobot()`
**Fix:** Use Fix1

### Issue: Robot stutters/moves erratically
**Cause:** State machine re-triggering movement on every loop
**Fix:** Use Fix2

### Issue: Robot moves in wrong direction
**Cause:** Motor directions reversed or wrong sign on angle
**Fix:** 
1. Run Fix3 to verify motor directions
2. Change `-40.0` to `40.0` in `rotateExactFast()` if turning wrong way
3. Swap `FORWARD`/`REVERSE` in `initHardware()` if motors reversed

### Issue: Robot doesn't move at all
**Cause:** Motors not responding to `RUN_TO_POSITION`
**Fix:**
1. Check encoder cables
2. Verify motors are in `RUN_TO_POSITION` mode before setting power
3. Check if `isBusy()` is working correctly

---

## Quick Reference: Motor Direction Configurations

If your robot behaves unexpectedly, try these direction combinations in `initHardware()`:

### Standard Tank Drive (Try this first):
```java
leftDrive.setDirection(DcMotor.Direction.FORWARD);
rightDrive.setDirection(DcMotor.Direction.REVERSE);
```

### Alternative 1:
```java
leftDrive.setDirection(DcMotor.Direction.REVERSE);
rightDrive.setDirection(DcMotor.Direction.FORWARD);
```

### Alternative 2 (Both forward):
```java
leftDrive.setDirection(DcMotor.Direction.FORWARD);
rightDrive.setDirection(DcMotor.Direction.FORWARD);
```

**Note:** Your teleop code uses the "Standard Tank Drive" configuration.

---

## Files Reference

| File | Purpose |
|------|---------|
| `RED_AUTO_FAR_FIX1.java` | **PRIMARY FIX** - Adds missing while loop |
| `RED_AUTO_FAR_FIX2.java` | State machine improvements |
| `Motor_Direction_Test.java` | Diagnostic opmode for motor testing |
| `RED_AUTO_FAR_DEBUG.java` | Enhanced telemetry for debugging |

---

## Recommended Order of Testing

1. **Start with Fix3** - Run Motor Direction Test to confirm hardware setup
2. **Deploy Fix1** - Apply the primary fix for the while loop issue
3. **Test on field** - Check if robot executes movements correctly
4. If issues persist:
   - Try Fix2 for state machine improvements
   - Use Debug version to gather telemetry data

## Contact & Support
If problems persist after trying these fixes:
1. Record the debug telemetry output
2. Note exact robot behavior (which way it spins, speed, etc.)
3. Check encoder wire connections
4. Verify control hub firmware is up to date
