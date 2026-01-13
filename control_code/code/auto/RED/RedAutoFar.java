package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.ExposureControl;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.GainControl;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.List;
import java.util.concurrent.TimeUnit;

@TeleOp(name = "Red Team Far", group = "Red")
public class RedAutoFar extends LinearOpMode {

    // --- Hardware ---
    DcMotor leftDrive, rightDrive, intakeDrive;
    DcMotorEx shooter;
    Servo feeder, angle;
    AprilTagProcessor aprilTag;
    VisionPortal visionPortal;

    // --- State Machine ---
    enum AutoState { IDLE, SCAN_CENTER, SEARCH_SHOOTER, LOCK_SHOOTER, TURN_BACK, SHOOTING, COLLECT_BALLS }
    AutoState currentState = AutoState.IDLE;
    
    int ballBatchCount = 0;
    static final int MAX_BALL_BATCHES = 3;

    // --- Constants ---
    static final double TRACK_WIDTH_INCH = 16.0;
    static final double TICKS_PER_REV = 420;
    static final double WHEEL_DIAMETER_INCH = 3.543;
    static final double TICKS_PER_INCH = TICKS_PER_REV / (Math.PI * WHEEL_DIAMETER_INCH);
    static final double TICKS_PER_DEGREE = (TRACK_WIDTH_INCH * Math.PI * TICKS_PER_INCH) / 360.0;
    
    static final double GEAR_RATIO = 15.0 / 10.0;
    static final double SHOOTER_TICKS_PER_REV = 28;
    static final double HEADING_TOLERANCE = 1.5;
    static final double DISTANCE_TOLERANCE = 1.0;

    long lastLockTime = 0;

    @Override
    public void runOpMode() {
        initHardware();
        initAprilTag();

        telemetry.addLine("READY - TAHAN LEFT BUMPER");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {

            // 🔒 RE-LOCK EXPOSURE TIAP 500 ms
            if (System.currentTimeMillis() - lastLockTime > 500) {
                lockCameraExposure();
                lastLockTime = System.currentTimeMillis();
            }

            AprilTagDetection tag21 = getSpecificTag(21);
            AprilTagDetection tag24 = getSpecificTag(24);

            if (gamepad1.left_bumper) {
                runAutoLogic(tag21, tag24);
            } else {
                currentState = AutoState.IDLE;
                runManualControl();
            }

            displayTelemetry(tag21, tag24);
        }

    }

    void runAutoLogic(AprilTagDetection tag21, AprilTagDetection tag24) {
        switch (currentState) {
            case IDLE:
                angle.setPosition(0.4);
                feeder.setPosition(0.0);
                rotateExactFast(-40, 0.6); // Miring kiri awal
                driveStraight(0.5, 40);    // Maju kencang
                currentState = AutoState.SCAN_CENTER;
                break;

            case SCAN_CENTER:
                if (tag21 != null) {
                    // Begitu terlihat, STOP dulu agar posisi stabil
                    stopDrive();
                    sleep(150); // Jeda singkat agar kamera fokus
                    
                    double rangeError21 = tag21.ftcPose.range - 65;
                    if (Math.abs(rangeError21) > DISTANCE_TOLERANCE) {
                        driveStraight(0.4, rangeError21);
                    } 
                    else {
                        currentState = AutoState.SEARCH_SHOOTER;
                    }
                } else {
                    // Power spin dikecilkan menjadi 0.15 agar kamera sempat memproses gambar
                    setDrivePower(-0.18, 0.18); 
                }
                break;

            case SEARCH_SHOOTER:
                if (tag24 != null) {
                    // STOP seketika begitu terdeteksi
                    stopDrive();
                    sleep(200);
                    currentState = AutoState.LOCK_SHOOTER;
                } else {
                    // Putar sangat pelan (0.15) untuk mencari tag shooter
                    // Power kecil = Kamera lebih mudah "mengunci" objek
                    setDrivePower(0.16, -0.16); 
                }
                break;

            case LOCK_SHOOTER:
                if (tag24 != null) {
                    // shooter.setVelocity((6000.0 / 60.0) / GEAR_RATIO * SHOOTER_TICKS_PER_REV);
                    double rangeError24 = tag24.ftcPose.range - 68; // Target 100cm
                    
                    if (Math.abs(rangeError24) > DISTANCE_TOLERANCE) {
                        driveStraight(0.6, rangeError24);
                    } else {
                        currentState = AutoState.TURN_BACK;
                    }
                } else {
                    currentState = AutoState.SEARCH_SHOOTER;
                }
                break;

            case TURN_BACK:
                stopDrive();
                sleep(200);
                rotateExactFast(270, 0.6); // Putar balik (Back shooter)
                currentState = AutoState.SHOOTING;
                break;

            case SHOOTING:
                stopDrive();
                executeShoot(3);
                currentState = AutoState.COLLECT_BALLS;
                break;
                
            case COLLECT_BALLS:
                driveStraight(0.3, 20);
                rotateExactFast(-70, 0.6); // Miring kiri awal
                
                ballBatchCount++;
                // // 1. Putar balik menghadap tumpukan bola (intake di depan) 
                 
                // // 2. Tentukan jarak maju (tuning jarak di sini)
                double driveDist = 30.0; 
                if (ballBatchCount == 2) driveDist = 50.0;
                if (ballBatchCount == 3) driveDist = 80.0;

                driveStraight(0.3, driveDist);

                rotateExactFast(-120, 0.6);

                // // 3. Maju pelan sambil intake ON
                intakeDrive.setPower(1.0);
                driveStraight(0.3, 50);
                sleep(500);
                
                // // 4. Mundur kembali ke area scan
                intakeDrive.setPower(0);
                driveStraight(0.5, -50);
                
                rotateExactFast(-120, 0.6);
                
                // // 5. Cari gawang lagi untuk nembak bola yang baru diambil
                currentState = AutoState.SEARCH_SHOOTER;
                
                // break;
        }
    }

    void executeShoot(int count) {
        double targetWheelRPM = 6000.0;
        double motorRPM = targetWheelRPM / GEAR_RATIO;
        double targetTPS = (motorRPM / 60.0) * SHOOTER_TICKS_PER_REV;

        // shooter.setVelocity(targetTPS);
        angle.setPosition(0.3);
        sleep(1000); // Tunggu motor peak

        for (int i = 0; i < count; i++) {
            feeder.setPosition(0.4);
            sleep(400);
            feeder.setPosition(0.0);
            sleep(800);
        }
        shooter.setVelocity(0);
    }

    // --- DRIVE UTILS ---
    void rotateToHeadingFast(double bearing, double pwr) {
        int ticks = (int)(bearing * TICKS_PER_DEGREE);
        moveRobot(ticks, -ticks, pwr); 
    }
    
    void rotateExactFast(double degrees, double pwr) {
        int ticks = (int)(degrees * TICKS_PER_DEGREE);
        moveRobot(ticks, -ticks, pwr); 
    }

    void driveStraight(double pwr, double inch) {
        int ticks = (int)(inch * TICKS_PER_INCH);
        moveRobot(ticks, ticks, pwr);
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
    // 1. Define the Camera Pose
    Position cameraPosition = new Position(DistanceUnit.INCH, 0, 0, 0, 0);
    YawPitchRollAngles cameraOrientation = new YawPitchRollAngles(AngleUnit.DEGREES, 0, 0, 90, 0);

    // 2. Create the processor
    aprilTag = new AprilTagProcessor.Builder()
            .setCameraPose(cameraPosition, cameraOrientation)
            .build();

    // 3. Build the VisionPortal
    visionPortal = new VisionPortal.Builder()
            .setCamera(hardwareMap.get(WebcamName.class, "Webcam 1"))
            .addProcessor(aprilTag)
            .build();

    // --- PROSES SETTING EXPOSURE AGAR TIDAK PUTIH ---
    
    // Tunggu sampai kamera benar-benar siap streaming
  // Tunggu kamera streaming
    // Tunggu kamera streaming
    while (visionPortal.getCameraState() != VisionPortal.CameraState.STREAMING && !isStopRequested()) {
        sleep(20);
    }

    // BIARIN AUTO SETTLE DULU
    sleep(2000);

    // KUNCI MANUAL PERTAMA KALI
    lockCameraExposure();

    telemetry.addLine("Camera locked (init)");
    telemetry.update();


}

void lockCameraExposure() {
    try {
        ExposureControl exposureControl =
                visionPortal.getCameraControl(ExposureControl.class);
        GainControl gainControl =
                visionPortal.getCameraControl(GainControl.class);

        // PAKSA MANUAL TERUS
        exposureControl.setMode(ExposureControl.Mode.Manual);

        // NILAI STABIL FTC (LAPANGAN TERANG)
        exposureControl.setExposure(6, TimeUnit.MILLISECONDS);
        gainControl.setGain(8);
    } catch (Exception e) {
        // sengaja dikosongkan
    }
}


    AprilTagDetection getSpecificTag(int id) {
        List<AprilTagDetection> detections = aprilTag.getDetections();
        for (AprilTagDetection det : detections) {
            if (det.metadata != null && det.id == id) return det;
        }
        return null;
    }

    void setDrivePower(double lp, double rp) {
        leftDrive.setPower(lp);
        rightDrive.setPower(rp);
    }

    void stopDrive() { setDrivePower(0, 0); }

    void displayTelemetry(AprilTagDetection t21, AprilTagDetection t24) {
        telemetry.addData("State", currentState);
        if (t21 != null) telemetry.addData("Tag 21 Dist", t21.ftcPose.range);
        if (t24 != null) telemetry.addData("Tag 24 Dist", t24.ftcPose.range);
        telemetry.update();
    }
}