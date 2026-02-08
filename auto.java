package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;

@Autonomous(name ="SimpleMoveAuto", group = "Red")
public class auto extends LinearOpMode {

    // Hardware
    DcMotor leftDrive, rightDrive;
    DcMotorEx shooter;
    Servo feeder, angle;

    // Constants
    static final double WHEEL_DIAMETER_INCH = 3.543;
    static final double TICKS_PER_REV = 420;
    static final double TICKS_PER_INCH = TICKS_PER_REV / (Math.PI * WHEEL_DIAMETER_INCH);

    @Override
    public void runOpMode() {
        // 1. Initialize Hardware
        leftDrive = hardwareMap.get(DcMotor.class, "left_drive");
        rightDrive = hardwareMap.get(DcMotor.class, "right_drive");
        shooter = hardwareMap.get(DcMotorEx.class, "shooter_motor");
        feeder = hardwareMap.get(Servo.class, "feeder");
        angle = hardwareMap.get(Servo.class, "angle");

        leftDrive.setDirection(DcMotor.Direction.FORWARD);
        rightDrive.setDirection(DcMotor.Direction.REVERSE);

        telemetry.addLine("Ready to Start");
        telemetry.update();

        // 2. WAIT FOR START (Crucial for safety!)
        waitForStart();

        if (opModeIsActive()) {
            // 3. SEQUENCE OF ACTIONS
            
            // Set initial positions
            angle.setPosition(0.4);
            feeder.setPosition(0.0);

            // Drive Forward 24 inches at 50% power
            driveStraight(0.5, 24);

            // // Turn (approximate 90 degrees - adjust inches as needed)
            // // To turn right: left moves forward, right moves back
            // moveRobot((int)(10 * TICKS_PER_INCH), (int)(-10 * TICKS_PER_INCH), 0.4);

            // // Drive Forward another 10 inches   
            // driveStraight(0.5, 10);
            
            telemetry.addLine("Path Complete");
            telemetry.update();
            sleep(2000); 
        }
    }

    // --- HELPER METHODS ---

    public void driveStraight(double pwr, double inch) {
        int ticks = (int)(inch * TICKS_PER_INCH);
        moveRobot(ticks, ticks, pwr);
    }

    public void moveRobot(int leftTicks, int rightTicks, double pwr) {
        leftDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        leftDrive.setTargetPosition(leftTicks);
        rightDrive.setTargetPosition(rightTicks);

        leftDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rightDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        leftDrive.setPower(pwr);
        rightDrive.setPower(pwr);

        // Wait until the robot reaches the target
        while (opModeIsActive() && (leftDrive.isBusy() || rightDrive.isBusy())) {
            idle();
        }

        // Stop all motion
        leftDrive.setPower(0);
        rightDrive.setPower(0);

        // Reset to normal mode
        leftDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }
}