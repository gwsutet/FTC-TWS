package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.ExposureControl;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.GainControl;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Autonomous(name = "AutoLogic Real - Red Far", group = "Red")
public class RED_SAMPING1 extends LinearOpMode {

    // --- Hardware ---
    private DcMotor leftDrive, rightDrive, intakeDrive;
    private DcMotorEx shooter;
    private Servo feeder, angle;
    private AprilTagProcessor aprilTag;
    private VisionPortal visionPortal;

    private int shootingTargetId = 0;

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
    static final double GEAR_RATIO = 15.0 / 10.0;
    static final double SHOOTER_TICKS_PER_REV = 28;

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
 
            runAutoLogic(tag21, tag24);
            displayTelemetry(tag21, tag24);
        }
    }

    void runAutoLogic(AprilTagDetection tag21, AprilTagDetection tag24) {
        AprilTagDetection tag20 = getSpecificTag(20);
        AprilTagDetection tag22 = getSpecificTag(22);
        AprilTagDetection tag23 = getSpecificTag(23);
        AprilTagDetection centerTag = getCenterTag(tag21, tag22, tag23);

        AprilTagDetection shootingTarget;

        switch (currentState) {
            case IDLE:
                angle.setPosition(0.4);
                feeder.setPosition(0.0);

                rotateExactFast(-180.0, 0.9);
                driveStraight(0.9, 80.0);
                rotateExactFast(170.0, 1.0);
                driveStraight(0.9, -90.0);
                currentState = AutoState.SCAN_CENTER;
                break;

            case SCAN_CENTER:
                if (centerTag != null) {
                    double yawError = centerTag.ftcPose.bearing;
                    if (Math.abs(yawError) > 5.0) {
                        setDrivePower(-0.25, 0.25);
                    } else {
                        stopDrive();
                        double rangeError21 = centerTag.ftcPose.range - 60.0;
                        if (Math.abs(rangeError21) > DISTANCE_TOLERANCE) {
                            driveStraight(0.9, rangeError21);
                        } else {
                            currentState = AutoState.SEARCH_SHOOTER;
                        }
                    }
                } else {
                    setDrivePower(-0.18, 0.18);
                }
                break;

            case SEARCH_SHOOTER:
                if (tag24 != null) {
                    shootingTargetId = 24;
                } 

                // shootingTarget = (shootingTargetId == 24) ? tag24 ;
                if (shootingTarget != null) {
                    double yawError = shootingTarget.ftcPose.bearing;
                    if (Math.abs(yawError) > 5.0) {
                        setDrivePower(0.3, -0.3);
                    } else {
                        stopDrive();
                        currentState = AutoState.LOCK_SHOOTER;
                    }
                } else {
                    setDrivePower(0.18, -0.18);
                }
                break;

            case LOCK_SHOOTER:
                if (shootingTargetId == 24) shootingTarget = tag24;
                else if (shootingTargetId == 20) shootingTarget = tag20;
                else shootingTarget = null;

                if (shootingTarget != null) {
                    double yawError = shootingTarget.ftcPose.bearing;
                    if (Math.abs(yawError) > 3.0) {
                        setDrivePower(0.25, -0.25);
                    } else {
                        double rangeError = shootingTarget.ftcPose.range - 68.0;
                        if (Math.abs(rangeError) > DISTANCE_TOLERANCE) {
                            driveStraight(0.9, rangeError);
                        } else {
                            currentState = AutoState.TURN_BACK;
                        }
                    }
                } else {
                    shootingTargetId = 0;
                    currentState = AutoState.SEARCH_SHOOTER;
                }
                break;

            case TURN_BACK:
                rotateExactFast(-180.0, 0.9);
                currentState = AutoState.SHOOTING;
                break;

            case SHOOTING:
                stopDrive();
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

        double driveDist = 55.0;

        rotateExactFast(-79.2, 1.0);
        driveStraight(0.8, driveDist);
        rotateExactFast(-180.0, 1.0);

        intakeDrive.setPower(1.0);
        driveStraight(0.8, 50.0);
        driveStraight(0.3, 70.0);
        sleep(1000);
        intakeDrive.setPower(0);

        driveStraight(0.8, -120.0);
        rotateExactFast(-180.0, 1.0);
        driveStraight(0.8, driveDist);
        rotateExactFast(45.0, 1.0);

        currentState = AutoState.SEARCH_SHOOTER;
    }

    // --- SUB-ROUTINES ---

    void executeShoot(int count) {
        double targetWheelRPM = 6000.0;
        double motorRPM = targetWheelRPM / GEAR_RATIO;
        double targetTPS = (motorRPM / 60.0) * SHOOTER_TICKS_PER_REV;

        shooter.setVelocity(targetTPS);
        angle.setPosition(0.3);
        sleep(1000); // Allow spin up
        for (int i = 0; i < count; i++) {
            feeder.setPosition(0.4);
            sleep(400);
            feeder.setPosition(0.0);
            sleep(800);
        }
        shooter.setVelocity(0);
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

        shooter.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        shooter.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
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

        while (visionPortal.getCameraState() != VisionPortal.CameraState.STREAMING && !isStopRequested()) {
            sleep(20);
        }

        sleep(500);

        try {
            ExposureControl exposureControl = visionPortal.getCameraControl(ExposureControl.class);
            GainControl gainControl = visionPortal.getCameraControl(GainControl.class);

            if (exposureControl != null && gainControl != null) {
                exposureControl.setMode(ExposureControl.Mode.Manual);
                sleep(50);
                exposureControl.setExposure(15, TimeUnit.MILLISECONDS);
                gainControl.setGain(100);
                telemetry.addLine("Camera: Manual mode OK");
            } else {
                telemetry.addLine("ERROR: Camera controls unavailable");
            }
        } catch (Exception e) {
            telemetry.addLine("Camera error: " + e.getMessage());
        }
        telemetry.update();
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

    // Helper to get the first visible center tag (21, 22, 23)
    private AprilTagDetection getCenterTag(AprilTagDetection tag21, AprilTagDetection tag22, AprilTagDetection tag23) {
        if (tag21 != null) return tag21;
        if (tag22 != null) return tag22;
        if (tag23 != null) return tag23;
        return null;
    }
}