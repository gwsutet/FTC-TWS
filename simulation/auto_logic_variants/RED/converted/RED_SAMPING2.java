package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.Range;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;

@Autonomous(name = "AutoLogic Real - Red Far V2", group = "Red")
public class RedAutoFarV2 extends LinearOpMode {

    // States matching your enum
    enum AutoState { IDLE, SCAN_CENTER, SEARCH_SHOOTER, LOCK_SHOOTER, TURN_BACK, SHOOTING, COLLECT_BALLS }
    
    private AutoState currentState = AutoState.IDLE;
    private int ballBatchCount = 0;
    private HardwareSetup robot = new HardwareSetup(); // Assume a standard hardware helper class

    @Override
    public void runOpMode() {
        robot.init(hardwareMap);
        
        telemetry.addData("Status", "Ready");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            AprilTagDetection tag21 = robot.getAprilTag(21);
            AprilTagDetection tag24 = robot.getAprilTag(24);

            switch (currentState) {
                case IDLE:
                    runInitialSequence();
                    currentState = AutoState.SCAN_CENTER;
                    break;

                case SCAN_CENTER:
                    handleTagAlignment(tag21, 60.0, AutoState.SEARCH_SHOOTER, -0.18);
                    break;

                case SEARCH_SHOOTER:
                    // Scanning right (positive/negative power depends on your drivetrain orientation)
                    if (tag24 != null) {
                        if (Math.abs(tag24.ftcPose.bearing) < 5) currentState = AutoState.LOCK_SHOOTER;
                        else robot.drive.tank(0.3, -0.3);
                    } else {
                        robot.drive.tank(0.18, -0.18);
                    }
                    break;

                case LOCK_SHOOTER:
                    if (tag24 == null) {
                        currentState = AutoState.SEARCH_SHOOTER;
                    } else {
                        double bearing = tag24.ftcPose.bearing;
                        double rangeErr = tag24.ftcPose.range - 68.0;
                        
                        if (Math.abs(bearing) > 3.0) {
                            robot.drive.tank(0.25, -0.25);
                        } else if (Math.abs(rangeErr) > 1.0) {
                            driveStraight(0.9, rangeErr);
                        } else {
                            currentState = AutoState.TURN_BACK;
                        }
                    }
                    break;

                case TURN_BACK:
                    rotateExact(-180, 0.9);
                    currentState = AutoState.SHOOTING;
                    break;

                case SHOOTING:
                    robot.shooter.shoot(3); // 2.0 second internal block
                    currentState = AutoState.COLLECT_BALLS;
                    break;

                case COLLECT_BALLS:
                    runCollectSequence();
                    // Returns to SEARCH_SHOOTER at the end of the sequence
                    break;
            }
            
            displayTelemetry(tag21, tag24);
        }
    }

    // --- Sequence Methods ---

    private void runInitialSequence() {
        driveStraight(0.9, -80.0);
        rotateExact(-90.0, 1.0);  // Changed from 120 to -90 per your update
        driveStraight(0.9, -90.0);
    }

    private void runCollectSequence() {
        ballBatchCount++;
        double distFinal = 55.0;

        rotateExact(-79.2, 1.0);
        driveStraight(0.8, distFinal);
        rotateExact(-180.0, 1.0);
        
        // Intake Step
        robot.intake.setPower(1.0);
        driveStraight(0.8, 50.0);
        driveStraight(0.3, 70.0);
        sleep(1000); // beginSleep(1.0)
        robot.intake.setPower(0);
        
        driveStraight(0.8, -120.0);
        rotateExact(-180.0, 1.0);
        driveStraight(0.8, distFinal);
        rotateExact(45.0, 1.0);
        
        currentState = AutoState.SEARCH_SHOOTER;
    }

    // --- Helper Drivetrain Methods ---

    private void driveStraight(double power, double inches) {
        // Implementation using encoders (RunToPosition)
        robot.drive.moveInches(power, inches); 
        while (opModeIsActive() && robot.drive.isBusy()) idle();
        robot.drive.stop();
    }

    private void rotateExact(double degrees, double power) {
        robot.drive.turnDegrees(power, degrees);
        while (opModeIsActive() && robot.drive.isBusy()) idle();
        robot.drive.stop();
    }

    private void handleTagAlignment(AprilTagDetection tag, double targetRange, AutoState nextState, double scanPower) {
        if (tag != null) {
            if (Math.abs(tag.ftcPose.bearing) > 5.0) {
                robot.drive.tank(scanPower * 1.5, -scanPower * 1.5);
            } else {
                double rangeErr = tag.ftcPose.range - targetRange;
                if (Math.abs(rangeErr) > 1.0) driveStraight(0.9, rangeErr);
                else currentState = nextState;
            }
        } else {
            robot.drive.tank(scanPower, -scanPower);
        }
    }
}