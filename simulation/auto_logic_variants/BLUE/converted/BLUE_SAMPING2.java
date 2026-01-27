package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;

@Autonomous(name = "Blue Auto - Dynamic Hunter", group = "Blue")
public class BlueHunterAuto extends LinearOpMode {

    enum AutoState { IDLE, SCAN_CENTER, SEARCH_SHOOTER, LOCK_SHOOTER, TURN_BACK, SHOOTING, COLLECT_BALLS }
    
    private AutoState currentState = AutoState.IDLE;
    private AprilTagDetection shootingTarget = null;
    private HardwareSetup robot = new HardwareSetup(); 

    @Override
    public void runOpMode() {
        robot.init(hardwareMap);
        telemetry.addLine("Ready: Dynamic Target Mode (20/24)");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            // Update all potential tag visibility
            AprilTagDetection tag21 = robot.getAprilTag(21);
            AprilTagDetection tag20 = robot.getAprilTag(20);
            AprilTagDetection tag24 = robot.getAprilTag(24);

            switch (currentState) {
                case IDLE:
                    // Your complex initial sequence: Drive -80, Rotate -120, Drive -90
                    driveStraight(0.9, -80.0);
                    rotateExact(-120.0, 1.0);
                    driveStraight(0.9, -90.0);
                    currentState = AutoState.SCAN_CENTER;
                    break;

                case SCAN_CENTER:
                    // Align with Tag 21
                    if (tag21 != null) {
                        double bearing = tag21.ftcPose.bearing;
                        if (Math.abs(bearing) > 5.0) {
                            robot.drive.tank(0.25, -0.25); // Clockwise adjustment
                        } else {
                            double rangeErr = tag21.ftcPose.range - 60.0;
                            if (Math.abs(rangeErr) > 1.0) driveStraight(0.9, rangeErr);
                            else currentState = AutoState.SEARCH_SHOOTER;
                        }
                    } else {
                        robot.drive.tank(0.18, -0.18); // Scan right
                    }
                    break;

                case SEARCH_SHOOTER:
                    // Prioritize Tag 20, then Tag 24
                    shootingTarget = (tag20 != null) ? tag20 : (tag24 != null ? tag24 : null);

                    if (shootingTarget != null) {
                        double bearing = shootingTarget.ftcPose.bearing;
                        if (Math.abs(bearing) > 5.0) {
                            robot.drive.tank(-0.3, 0.3); // Rotate left to face target
                        } else {
                            currentState = AutoState.LOCK_SHOOTER;
                        }
                    } else {
                        robot.drive.tank(-0.18, 0.18); // Keep scanning left
                    }
                    break;

                case LOCK_SHOOTER:
                    // Verify target is still visible
                    shootingTarget = (tag20 != null) ? tag20 : (tag24 != null ? tag24 : null);

                    if (shootingTarget == null) {
                        currentState = AutoState.SEARCH_SHOOTER;
                    } else {
                        double bearing = shootingTarget.ftcPose.bearing;
                        double rangeErr = shootingTarget.ftcPose.range - 68.0;

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
                    rotateExact(180.0, 0.9);
                    currentState = AutoState.SHOOTING;
                    break;

                case SHOOTING:
                    robot.shooter.shoot(3);
                    currentState = AutoState.COLLECT_BALLS;
                    break;

                case COLLECT_BALLS:
                    runCollectSequence();
                    shootingTarget = null; // Reset for next batch
                    break;
            }
        }
    }

    // --- Helper Methods ---

    private void runCollectSequence() {
        rotateExact(79.2, 1.0);
        driveStraight(0.8, 55.0);
        rotateExact(180.0, 1.0);
        
        robot.intake.setPower(1.0);
        driveStraight(0.8, 50.0);
        driveStraight(0.3, 70.0);
        sleep(1000); 
        robot.intake.setPower(0);
        
        driveStraight(0.8, -120.0);
        rotateExact(180.0, 1.0);
        driveStraight(0.8, 55.0);
        rotateExact(-45.0, 1.0);
        
        currentState = AutoState.SEARCH_SHOOTER;
    }

    private void driveStraight(double pwr, double dist) {
        robot.drive.moveInches(pwr, dist);
        while (opModeIsActive() && robot.drive.isBusy()) idle();
        robot.drive.stop();
    }

    private void rotateExact(double deg, double pwr) {
        robot.drive.turnDegrees(pwr, deg);
        while (opModeIsActive() && robot.drive.isBusy()) idle();
        robot.drive.stop();
    }
}