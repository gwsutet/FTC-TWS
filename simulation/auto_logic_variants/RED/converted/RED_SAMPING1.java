package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.List;

@Autonomous(name = "AutoLogic Real - Red Far", group = "Red")
public class RedAutoFar extends LinearOpMode {

    // --- Hardware ---
    private DcMotor leftDrive, rightDrive, intakeDrive;
    private DcMotorEx shooter;
    private Servo feeder, angle;
    private AprilTagProcessor aprilTag;
    private VisionPortal visionPortal;

    // --- State Machine ---
    enum AutoState { IDLE, SCAN_CENTER, SEARCH_SHOOTER, LOCK_SHOOTER, TURN_BACK, SHOOTING, COLLECT_BALLS }
    AutoState currentState = AutoState.IDLE;
    int ballBatchCount = 0;

    // --- Robot Constants ---
    static final double TRACK_WIDTH_INCH = 16.0;
    static final double TICKS_PER_REV = 420;
    static final double WHEEL_DIAMETER_INCH = 3.543;
    static final double TICKS_PER_INCH = TICKS_PER_REV / (Math.PI * WHEEL_DIAMETER_INCH);
    static final double TICKS_PER_DEGREE = (TRACK_WIDTH_INCH * Math.PI * TICKS_PER_INCH) / 360.0;
    static final double DISTANCE_TOLERANCE = 1.0;

    @Override
    public void runOpMode() {
        initHardware();
        initAprilTag();

        telemetry.addLine("READY - Hold Left Bumper to Execute");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            AprilTagDetection tag21 = getSpecificTag(21);
            AprilTagDetection tag24 = getSpecificTag(24);

            if (gamepad1.left_bumper) {
                runAutoLogic(tag21, tag24);
            } else {
                // Manual reset/control if bumper is released
                stopDrive();
                runManualControl();
            }
            displayTelemetry(tag21, tag24);
        }
    }

    void runAutoLogic(AprilTagDetection tag21, AprilTagDetection tag24) {
        switch (currentState) {
            case IDLE:
                // Updated startInitialSequence logic
                driveStraight(0.6, -80.0);
                rotateExactFast(120.0, 0.6);
                driveStraight(0.6, -90.0);
                currentState = AutoState.SCAN_CENTER;
                break;

            case SCAN_CENTER:
                if (tag21 != null) {
                    double yawError = tag21.ftcPose.bearing;
                    if (Math.abs(yawError) > 5.0) {
                        setDrivePower(-0.25, 0.25); // Turn to face tag
                    } else {
                        stopDrive();
                        double rangeError = tag21.ftcPose.range - 60.0;
                        if (Math.abs(rangeError) > DISTANCE_TOLERANCE) {
                            driveStraight(0.4, rangeError);
                        } else {
                            currentState = AutoState.SEARCH_SHOOTER;
                        }
                    }
                } else {
                    setDrivePower(-0.18, 0.18); // Scan left until found
                }
                break;

            case SEARCH_SHOOTER:
                if (tag24 != null) {
                    double yawError = tag24.ftcPose.bearing;
                    if (Math.abs(yawError) > 5.0) {
                        setDrivePower(0.3, -0.3); // Face tag 24
                    } else {
                        stopDrive();
                        currentState = AutoState.LOCK_SHOOTER;
                    }
                } else {
                    setDrivePower(0.18, -0.18); // Scan right until found
                }
                break;

            case LOCK_SHOOTER:
                if (tag24 != null) {
                    double yawError = tag24.ftcPose.bearing;
                    if (Math.abs(yawError) > 3.0) {
                        setDrivePower(0.25, -0.25);
                    } else {
                        double rangeError = tag24.ftcPose.range - 68.0;
                        if (Math.abs(rangeError) > DISTANCE_TOLERANCE) {
                            driveStraight(0.6, rangeError);
                        } else {
                            currentState = AutoState.TURN_BACK;
                        }
                    }
                } else {
                    currentState = AutoState.SEARCH_SHOOTER;
                }
                break;

            case TURN_BACK:
                rotateExactFast(-180.0, 0.8);
                currentState = AutoState.SHOOTING;
                break;

            case SHOOTING:
                executeShoot(3);
                currentState = AutoState.COLLECT_BALLS;
                break;

            case COLLECT_BALLS:
                runCollectSequence();
                break;
        }
    }

    private void runCollectSequence() {
        ballBatchCount++;
        double distFinal = 55.0;

        // Steps 1 & 2
        rotateExactFast(-79.2, 0.6);
        driveStraight(0.6, distFinal);
        
        // Step 3 & 4 (Intake movement)
        rotateExactFast(-180.0, 0.6);
        intakeDrive.setPower(1.0);
        driveStraight(0.8, 50.0); // Fast move
        driveStraight(0.3, 70.0); // Slow intake move
        sleep(1000);              // beginSleep(1.0)
        intakeDrive.setPower(0);

        // Step 5 & 6 (Return)
        driveStraight(0.6, -120.0);
        rotateExactFast(-180.0, 0.6);

        // Step 7 & 8 (Reset to Search)
        driveStraight(0.6, distFinal);
        rotateExactFast(45.0, 0.6);

        currentState = AutoState.SEARCH_SHOOTER;
    }

    // --- SUB-ROUTINES ---

    void executeShoot(int count) {
        shooter.setPower(0.85); 
        angle.setPosition(0.3);
        sleep(1000); // Allow spin up
        for (int i = 0; i < count; i++) {
            feeder.setPosition(0.4);
            sleep(400);
            feeder.setPosition(0.0);
            sleep(800);
        }
        shooter.setPower(0);
    }

    void driveStraight(double pwr, double inch) {
        int ticks = (int)(inch * TICKS_PER_INCH);
        moveRobot(ticks, ticks, pwr);
    }

    void rotateExactFast(double degrees, double pwr) {
        int ticks = (int)(degrees * TICKS_PER_DEGREE);
        moveRobot(ticks, -ticks, pwr);
    }

    void moveRobot(int leftTicks, int rightTicks, double pwr) {
        leftDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        leftDrive.setTargetPosition(leftTicks);
        rightDrive.setTargetPosition(rightTicks);
        leftDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rightDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        leftDrive.setPower(pwr);
        rightDrive.setPower(pwr);
        
        while (opModeIsActive() && (leftDrive.isBusy() || rightDrive.isBusy()) && gamepad1.left_bumper) {
            idle();
        }
        stopDrive();
        leftDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    void setDrivePower(double lp, double rp) {
        leftDrive.setPower(lp);
        rightDrive.setPower(rp);
    }

    void stopDrive() { setDrivePower(0, 0); }

    void runManualControl() {
        leftDrive.setPower(Range.clip(-gamepad1.left_stick_y + gamepad1.right_stick_x, -1.0, 1.0));
        rightDrive.setPower(Range.clip(-gamepad1.left_stick_y - gamepad1.right_stick_x, -1.0, 1.0));
    }

    void initHardware() {
        leftDrive = hardwareMap.get(DcMotor.class, "left_drive");
        rightDrive = hardwareMap.get(DcMotor.class, "right_drive");
        intakeDrive = hardwareMap.get(DcMotor.class, "intake_driver");
        shooter = hardwareMap.get(DcMotorEx.class, "shooter_motor");
        feeder = hardwareMap.get(Servo.class, "feeder");
        angle = hardwareMap.get(Servo.class, "angle");
        
        leftDrive.setDirection(DcMotor.Direction.FORWARD);
        rightDrive.setDirection(DcMotor.Direction.REVERSE);
    }

    void initAprilTag() {
        aprilTag = new AprilTagProcessor.Builder()
                .setCameraPose(new Position(DistanceUnit.INCH, 0, 0, 0, 0), 
                               new YawPitchRollAngles(AngleUnit.DEGREES, 0, 0, 90, 0))
                .build();
        visionPortal = new VisionPortal.Builder()
                .setCamera(hardwareMap.get(WebcamName.class, "Webcam 1"))
                .addProcessor(aprilTag)
                .build();
    }

    AprilTagDetection getSpecificTag(int id) {
        List<AprilTagDetection> detections = aprilTag.getDetections();
        for (AprilTagDetection det : detections) {
            if (det.metadata != null && det.id == id) return det;
        }
        return null;
    }

    void displayTelemetry(AprilTagDetection t21, AprilTagDetection t24) {
        telemetry.addData("State", currentState);
        telemetry.addData("Batches", ballBatchCount);
        if (t21 != null) telemetry.addData("T21 Range", t21.ftcPose.range);
        if (t24 != null) telemetry.addData("T24 Range", t24.ftcPose.range);
        telemetry.update();
    }
}