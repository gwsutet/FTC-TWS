package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotor;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;

@Autonomous(name = "Auto Red Far - Tag 20", group = "Red")
public class RedAutoTag20 extends LinearOpMode {

    enum AutoState { IDLE, SCAN_CENTER, SEARCH_SHOOTER, LOCK_SHOOTER, TURN_BACK, SHOOTING, COLLECT_BALLS }
    
    private AutoState currentState = AutoState.IDLE;
    private int ballBatchCount = 0;
    private HardwareSetup robot = new HardwareSetup(); 

    @Override
    public void runOpMode() {
        robot.init(hardwareMap);
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            // Using Tag 21 for Center and Tag 20 for Shooter
            AprilTagDetection tag21 = robot.getAprilTag(21);
            AprilTagDetection tag20 = robot.getAprilTag(20);

            switch (currentState) {
                case IDLE:
                    // New simplified sequence: Rotate 10, then drive 80
                    rotateExact(10.0, 1.0);
                    driveStraight(0.9, 80.0);
                    currentState = AutoState.SCAN_CENTER;
                    break;

                case SCAN_CENTER:
                    // Facing Tag 21 - Clockwise scan/turn
                    if (tag21 != null) {
                        double bearing = tag21.ftcPose.bearing;
                        if (Math.abs(bearing) > 5.0) {
                            robot.drive.tank(0.25, -0.25); // Clockwise
                        } else {
                            double rangeErr = tag21.ftcPose.range - 60.0;
                            if (Math.abs(rangeErr) > 1.0) driveStraight(0.9, rangeErr);
                            else currentState = AutoState.SEARCH_SHOOTER;
                        }
                    } else {
                        robot.drive.tank(0.18, -0.18); // Scan clockwise
                    }
                    break;

                case SEARCH_SHOOTER:
                    // Facing Tag 20 - Counter-clockwise scan/turn
                    if (tag20 != null) {
                        double bearing = tag20.ftcPose.bearing;
                        if (Math.abs(bearing) > 5.0) {
                            robot.drive.tank(-0.3, 0.3); 
                        } else {
                            currentState = AutoState.LOCK_SHOOTER;
                        }
                    } else {
                        robot.drive.tank(-0.18, 0.18); // Scan counter-clockwise
                    }
                    break;

                case LOCK_SHOOTER:
                    if (tag20 == null) {
                        currentState = AutoState.SEARCH_SHOOTER;
                    } else {
                        double bearing = tag20.ftcPose.bearing;
                        double rangeErr = tag20.ftcPose.range - 68.0;
                        
                        if (Math.abs(bearing) > 3.0) {
                            robot.drive.tank(-0.25, 0.25); // Fine alignment
                        } else if (Math.abs(rangeErr) > 1.0) {
                            driveStraight(0.9, rangeErr);
                        } else {
                            currentState = AutoState.TURN_BACK;
                        }
                    }
                    break;

                case TURN_BACK:
                    rotateExact(180, 0.9);
                    currentState = AutoState.SHOOTING;
                    break;

                case SHOOTING:
                    robot.shooter.shoot(3); // Blocks for 2s
                    currentState = AutoState.COLLECT_BALLS;
                    break;

                case COLLECT_BALLS:
                    runCollectSequence();
                    break;
            }
        }
    }

    private void runCollectSequence() {
        ballBatchCount++;
        double distFinal = 55.0;

        // Step 1 & 2
        rotateExact(79.2, 1.0);
        driveStraight(0.8, distFinal);
        
        // Step 3 & 4: Turn and Intake
        rotateExact(180.0, 1.0);
        robot.intake.setPower(1.0);
        driveStraight(0.8, 50.0);  // Fast
        driveStraight(0.3, 70.0);  // Slow
        sleep(1000);               // Sleep 1s
        robot.intake.setPower(0);
        
        // Step 5-8: Return
        driveStraight(0.8, -120.0);
        rotateExact(180.0, 1.0);
        driveStraight(0.8, distFinal);
        rotateExact(-45.0, 1.0);
        
        currentState = AutoState.SEARCH_SHOOTER;
    }

    private void driveStraight(double power, double inches) {
        robot.drive.moveInches(power, inches); 
        while (opModeIsActive() && robot.drive.isBusy()) idle();
        robot.drive.stop();
    }

    private void rotateExact(double degrees, double power) {
        robot.drive.turnDegrees(power, degrees);
        while (opModeIsActive() && robot.drive.isBusy()) idle();
        robot.drive.stop();
    }
}