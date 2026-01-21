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
import java.util.concurrent.TimeUnit;

@Autonomous(name = "AutoLogic Real Robot", group = "Red")
public class AutoLogicReal extends LinearOpMode {

    // --- Hardware ---
    private DcMotor leftDrive, rightDrive, intakeDrive;
    private DcMotorEx shooter;
    private Servo feeder, angle;
    private AprilTagProcessor aprilTag;
    private VisionPortal visionPortal;

    // --- PID Controllers ---
    private PIDController leftDrivePID, rightDrivePID, shooterPID;

    // --- Motor Constants ---
    static final double SHOOTER_TARGET_RPM = 6000; // Target RPM for shooter
    static final double SHOOTER_TICKS_PER_REV = 28; // Typical for shooter motors
    static final double SHOOTER_MAX_POWER = 0.85;

    // --- State Machine ---
    enum AutoState { IDLE, SCAN_CENTER, SEARCH_SHOOTER, LOCK_SHOOTER, TURN_BACK, SHOOTING, COLLECT_BALLS }
    AutoState currentState = AutoState.IDLE;

    int ballBatchCount = 0;

    // --- Constants ---
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

        telemetry.addLine("Ready to Start");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            AprilTagDetection tag21 = getSpecificTag(21);
            AprilTagDetection tag24 = getSpecificTag(24);

            // Update shooter RPM control continuously
            updateShooterControl();

            // Using the bumper as an "Engagement Switch" like in your example
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
                // StartInitialSequence logic
                rotateExactFast(-40.0, 0.6);
                driveStraight(0.6, 40.0);
                currentState = AutoState.SCAN_CENTER;
                break;

            case SCAN_CENTER:
                if (tag21 != null) {
                    double yawError = tag21.ftcPose.bearing; // Equivalent to tag21.yaw()
                    if (Math.abs(yawError) > 5.0) {
                        setDrivePower(-0.25, 0.25);
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
                    setDrivePower(-0.18, 0.18); // Scan left
                }
                break;

            case SEARCH_SHOOTER:
                if (tag24 != null) {
                    double yawError = tag24.ftcPose.bearing;
                    if (Math.abs(yawError) > 5.0) {
                        setDrivePower(0.3, -0.3);
                    } else {
                        stopDrive();
                        currentState = AutoState.LOCK_SHOOTER;
                    }
                } else {
                    setDrivePower(0.18, -0.18); // Scan right
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
                            driveStraight(0.4, rangeError);
                        } else {
                            currentState = AutoState.TURN_BACK;
                        }
                    }
                } else {
                    currentState = AutoState.SEARCH_SHOOTER;
                }
                break;

            case TURN_BACK:
                rotateExactFast(-180.0, 0.6);
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

    /**
     * Translated logic from simulator startCollectSequence steps
     */
    private void runCollectSequence() {
        ballBatchCount++;
        double driveDist = 55.0; // From simulator default

        // Step 1: Small turn
        rotateExactFast(-79.2, 0.6);
        // Step 2: Drive to center
        driveStraight(0.6, driveDist);
        // Step 3: Face ball stack
        rotateExactFast(-180.0, 0.6);
        
        // Step 4: Intake logic
        intakeDrive.setPower(1.0);
        driveStraight(0.8, 50.0); // Quick approach
        driveStraight(0.3, 70.0); // Slow intake drive
        sleep(1000); // beginSleep(1.0)
        intakeDrive.setPower(0);

        // Step 5 & 6: Return
        driveStraight(0.6, -120.0);
        rotateExactFast(-180.0, 0.6);
        
        // Step 7 & 8: Align and find shooter again
        driveStraight(0.6, driveDist);
        rotateExactFast(45.0, 0.6);
        
        currentState = AutoState.SEARCH_SHOOTER;
    }

    void executeShoot(int count) {
        // Use RPM control for shooter
        setShooterRPM(SHOOTER_TARGET_RPM);
        angle.setPosition(0.3);
        
        // Wait for shooter to reach target RPM
        while (opModeIsActive() && !isShooterAtTarget()) {
            idle();
        }
        
        for (int i = 0; i < count; i++) {
            feeder.setPosition(0.4);
            sleep(400);
            feeder.setPosition(0.0);
            sleep(800);
        }
        shooter.setPower(0);
    }

    // --- DRIVE UTILS ---
    void driveStraight(double pwr, double inch) {
        moveRobot((int)(inch * TICKS_PER_INCH), (int)(inch * TICKS_PER_INCH), pwr);
    }

    void rotateExactFast(double degrees, double pwr) {
        moveRobot((int)(degrees * TICKS_PER_DEGREE), (int)(-degrees * TICKS_PER_DEGREE), pwr);
    }

    void moveRobot(int leftTicks, int rightTicks, double pwr) {
        leftDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        leftDrive.setTargetPosition(leftTicks);
        rightDrive.setTargetPosition(rightTicks);
        leftDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rightDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        
        // Reset PID controllers
        leftDrivePID.reset();
        rightDrivePID.reset();
        leftDrivePID.setSetpoint(leftTicks);
        rightDrivePID.setSetpoint(rightTicks);
        
        while (opModeIsActive() && (leftDrive.isBusy() || rightDrive.isBusy()) && gamepad1.left_bumper) {
            // Use PID control for precise positioning
            double currentTime = System.currentTimeMillis();
            double leftPower = leftDrivePID.calculate(leftDrive.getCurrentPosition(), currentTime);
            double rightPower = rightDrivePID.calculate(rightDrive.getCurrentPosition(), currentTime);
            
            // Apply power limits
            leftPower = Range.clip(Math.abs(leftPower) * pwr, 0, pwr) * Math.signum(leftPower);
            rightPower = Range.clip(Math.abs(rightPower) * pwr, 0, pwr) * Math.signum(rightPower);
            
            leftDrive.setPower(leftPower);
            rightDrive.setPower(rightPower);
            
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
        
        // Initialize PID controllers
        leftDrivePID = new PIDController(0.05, 0.001, 0.02); // Tuned for position control
        rightDrivePID = new PIDController(0.05, 0.001, 0.02);
        shooterPID = new PIDController(0.002, 0.0001, 0.0005); // Tuned for velocity control
        
        // Configure motors for encoder control
        leftDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        shooter.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
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
        telemetry.addData("Batch", ballBatchCount);
        if (t21 != null) telemetry.addData("Tag 21 Dist", t21.ftcPose.range);
        if (t24 != null) telemetry.addData("Tag 24 Dist", t24.ftcPose.range);
        
        // PID debugging info
        telemetry.addData("Shooter RPM", getCurrentShooterRPM());
        telemetry.addData("Shooter Target", SHOOTER_TARGET_RPM);
        telemetry.addData("Left Pos", leftDrive.getCurrentPosition());
        telemetry.addData("Right Pos", rightDrive.getCurrentPosition());
        
        telemetry.update();
    }
    
    // --- RPM Control Methods ---
    private void setShooterRPM(double targetRPM) {
        shooterPID.setSetpoint(targetRPM);
    }
    
    private double getCurrentShooterRPM() {
        double ticksPerSecond = shooter.getVelocity();
        return (ticksPerSecond / SHOOTER_TICKS_PER_REV) * 60.0; // Convert to RPM
    }
    
    private boolean isShooterAtTarget() {
        double currentRPM = getCurrentShooterRPM();
        return Math.abs(currentRPM - SHOOTER_TARGET_RPM) < 50; // Within 50 RPM tolerance
    }
    
    private void updateShooterControl() {
        if (shooter.getPower() > 0) {
            double currentTime = System.currentTimeMillis();
            double currentRPM = getCurrentShooterRPM();
            double power = shooterPID.calculate(currentRPM, currentTime);
            shooter.setPower(Range.clip(power, 0, SHOOTER_MAX_POWER));
        }
    }

    // PID Controller for precise control
    class PIDController {
        private double kp, ki, kd;
        private double setpoint;
        private double integral = 0;
        private double previousError = 0;
        private double lastTime = 0;
        
        public PIDController(double kp, double ki, double kd) {
            this.kp = kp;
            this.ki = ki;
            this.kd = kd;
        }
        
        public void setSetpoint(double setpoint) {
            this.setpoint = setpoint;
        }
        
        public double calculate(double current, double currentTime) {
            double error = setpoint - current;
            double dt = (lastTime == 0) ? 0.02 : (currentTime - lastTime) / 1000.0; // Convert to seconds
            
            integral += error * dt;
            integral = Range.clip(integral, -1.0, 1.0); // Anti-windup
            
            double derivative = (dt > 0) ? (error - previousError) / dt : 0;
            
            double output = kp * error + ki * integral + kd * derivative;
            
            previousError = error;
            lastTime = currentTime;
            
            return Range.clip(output, -1.0, 1.0);
        }
        
        public void reset() {
            integral = 0;
            previousError = 0;
            lastTime = 0;
        }
    }
}