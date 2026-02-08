package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.ExposureControl;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.GainControl;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Autonomous(name = "AutoLogic Real - Blue Samping2 (FIXED)", group = "Blue")
public class BLUE_SAMPING2_FIX1 extends LinearOpMode {

    enum AutoState { IDLE, SCAN_CENTER, SEARCH_SHOOTER, LOCK_SHOOTER, TURN_BACK, SHOOTING, COLLECT_BALLS }

    private AutoState currentState = AutoState.IDLE;
    private int ballBatchCount = 0;
    static final int MAX_BALL_BATCHES = 3;
    Random random = new Random();

    private DcMotor leftDrive, rightDrive, intakeDrive;
    private DcMotorEx shooter;
    private Servo feeder, angle;
    private AprilTagProcessor aprilTag;
    private VisionPortal visionPortal;

    static final double TRACK_WIDTH_INCH = 16.0;
    static final double TICKS_PER_REV = 420;
    static final double WHEEL_DIAMETER_INCH = 3.543;
    static final double TICKS_PER_INCH = TICKS_PER_REV / (Math.PI * WHEEL_DIAMETER_INCH);
    static final double TICKS_PER_DEGREE = (TRACK_WIDTH_INCH * Math.PI * TICKS_PER_INCH) / 360.0;

    static final double GEAR_RATIO = 15.0 / 10.0;
    static final double SHOOTER_TICKS_PER_REV = 28;
    static final double DISTANCE_TOLERANCE = 1.0;

    @Override
    public void runOpMode() {
        initHardware();
        initAprilTag();
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            AprilTagDetection tag21 = getSpecificTag(21);
            AprilTagDetection tag22 = getSpecificTag(22);
            AprilTagDetection tag23 = getSpecificTag(23);
            AprilTagDetection tag20 = getSpecificTag(20);
            
            runAutoLogic(tag21, tag22, tag23, tag20);
            displayTelemetry(tag21, tag22, tag23, tag20);
        }
    }

    void runAutoLogic(AprilTagDetection tag21, AprilTagDetection tag22, AprilTagDetection tag23, AprilTagDetection tag20) {
        AprilTagDetection centerTag = getRandomCenterTag(tag21, tag22, tag23);

        switch (currentState) {
            case IDLE:
                angle.setPosition(0.4);
                feeder.setPosition(0.0);  
                driveStraight(0.9, -80.0);
                rotateExactFast(90.0, 1.0);
                driveStraight(0.9, -90.0);
                currentState = AutoState.SCAN_CENTER;
                break;

            case SCAN_CENTER:
                if (centerTag != null) {
                    double yawError = centerTag.ftcPose.bearing;
                    if (Math.abs(yawError) > 5.0) {
                        setDrivePower(0.25, -0.25);
                    } else {
                        stopDrive();
                        double rangeError = centerTag.ftcPose.range - 60.0;
                        if (Math.abs(rangeError) > DISTANCE_TOLERANCE) {
                            driveStraight(0.9, rangeError);
                        } else {
                            currentState = AutoState.SEARCH_SHOOTER;
                        }
                    }
                } else {
                    setDrivePower(0.18, -0.18);
                }
                break;

            case SEARCH_SHOOTER:
                if (tag20 != null) {
                    double yawError = tag20.ftcPose.bearing;
                    if (Math.abs(yawError) > 5.0) {
                        setDrivePower(-0.3, 0.3);
                    } else {
                        stopDrive();
                        currentState = AutoState.LOCK_SHOOTER;
                    }
                } else {
                    setDrivePower(-0.18, 0.18);
                }
                break;

            case LOCK_SHOOTER:
                if (tag20 != null) {
                    double yawError = tag20.ftcPose.bearing;
                    if (Math.abs(yawError) > 3.0) {
                        setDrivePower(-0.25, 0.25);
                    } else {
                        double rangeError = tag20.ftcPose.range - 68.0;
                        if (Math.abs(rangeError) > DISTANCE_TOLERANCE) {
                            driveStraight(0.9, rangeError);
                        } else {
                            currentState = AutoState.TURN_BACK;
                        }
                    }
                } else {
                    currentState = AutoState.SEARCH_SHOOTER;
                }
                break;

            case TURN_BACK:
                rotateExactFast(180.0, 0.9);
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

        rotateExactFast(79.2, 1.0);
        driveStraight(0.8, driveDist);
        rotateExactFast(180.0, 1.0);

        intakeDrive.setPower(1.0);
        driveStraight(0.8, 50.0);
        driveStraight(0.3, 70.0);
        sleep(1000);
        intakeDrive.setPower(0);

        driveStraight(0.8, -120.0);
        rotateExactFast(180.0, 1.0);
        driveStraight(0.8, driveDist);
        rotateExactFast(-45.0, 1.0);

        currentState = AutoState.SEARCH_SHOOTER;
    }

    void executeShoot(int count) {
        double targetWheelRPM = 6000.0;
        double motorRPM = targetWheelRPM / GEAR_RATIO;
        double targetTPS = (motorRPM / 60.0) * SHOOTER_TICKS_PER_REV;

        shooter.setVelocity(targetTPS);
        angle.setPosition(0.3);
        sleep(1000);

        for (int i = 0; i < count; i++) {
            feeder.setPosition(0.4);
            sleep(400);
            feeder.setPosition(0.0);
            sleep(800);
        }
        shooter.setVelocity(0);
    }

    void rotateExactFast(double degrees, double pwr) {
        int ticks = (int)(degrees * TICKS_PER_DEGREE);
        moveRobot(ticks, -ticks, pwr);
    }

    void driveStraight(double pwr, double inch) {
        int ticks = (int)(inch * TICKS_PER_INCH);
        moveRobot(ticks, ticks, pwr);
    }

    /**
     * FIX: Added while loop to wait for motors to reach target position
     * The original code was missing this, causing motors to stop immediately
     */
    void moveRobot(int leftTicks, int rightTicks, double pwr) {
        leftDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        leftDrive.setTargetPosition(leftTicks);
        rightDrive.setTargetPosition(rightTicks);
        leftDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rightDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        leftDrive.setPower(pwr);
        rightDrive.setPower(pwr);

        // FIX: Wait for motors to reach target - THIS WAS MISSING!
        while (opModeIsActive() && (leftDrive.isBusy() || rightDrive.isBusy())) {
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
        double drive = -gamepad1.left_stick_y;
        double turn  = gamepad1.right_stick_x;
        leftDrive.setPower(Range.clip(drive + turn, -1.0, 1.0));
        rightDrive.setPower(Range.clip(drive - turn, -1.0, 1.0));

        if (gamepad1.a) intakeDrive.setPower(1.0);
        else if (gamepad1.b) intakeDrive.setPower(-1.0);
        else intakeDrive.setPower(0);

        if (gamepad1.right_trigger > 0.1) {
            shooter.setVelocity((gamepad1.right_trigger * 6000.0 / 60.0) * 28);
        } else {
            shooter.setVelocity(0);
        }
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
        Position cameraPosition = new Position(DistanceUnit.INCH, 0, 0, 0, 0);
        YawPitchRollAngles cameraOrientation = new YawPitchRollAngles(AngleUnit.DEGREES, 0, 90, 0, 0);

        aprilTag = new AprilTagProcessor.Builder()
                .setCameraPose(cameraPosition, cameraOrientation)
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

    void displayTelemetry(AprilTagDetection t21, AprilTagDetection t22, AprilTagDetection t23, AprilTagDetection t20) {
        telemetry.addData("State", currentState);
        telemetry.addData("Batches", ballBatchCount);
        if (t21 != null) telemetry.addData("T21 Range", t21.ftcPose.range);
        if (t22 != null) telemetry.addData("T22 Range", t22.ftcPose.range);
        if (t23 != null) telemetry.addData("T23 Range", t23.ftcPose.range);
        if (t20 != null) telemetry.addData("T20 Range", t20.ftcPose.range);
        telemetry.update();
    }

    private AprilTagDetection getRandomCenterTag(AprilTagDetection tag21, AprilTagDetection tag22, AprilTagDetection tag23) {
        List<AprilTagDetection> availableTags = new java.util.ArrayList<>();
        if (tag21 != null) availableTags.add(tag21);
        if (tag22 != null) availableTags.add(tag22);
        if (tag23 != null) availableTags.add(tag23);
        
        if (availableTags.isEmpty()) return null;
        return availableTags.get(random.nextInt(availableTags.size()));
    }
}
