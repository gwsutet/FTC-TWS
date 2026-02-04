package org.firstinspires.ftc.robotcontroller.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;
  

@TeleOp(name="basic_OPMode", group="Iterative OpMode")
public class mainOpMode extends OpMode
{
    private ElapsedTime runtime = new ElapsedTime();
    private DcMotor leftDrive = null;
    private DcMotor rightDrive = null;
    private DcMotor intakeDrive = null;
    private DcMotorEx shooter = null;
    private Servo feeder = null;
    private Servo angle =   null;
    static final double TICKS_PER_REV = 28;
    static final double MAX_RPM = 6000;

    double angleDegree = 0.4;        // posisi awal servo (0–1)
    static final double ANGLE_STEP = 0.05; 

    static final double ANGLE_MIN = 0.10;
    static final double ANGLE_MAX = 0.80;

    boolean dpadUpLast = false;
    boolean dpadDownLast = false;

    int intakeMode = 0;
    boolean intakeOn = false;
    boolean OutTakeOn = false;
    boolean feederOn = false; 
    boolean angleOn = false;
    boolean xLast = false;
    boolean yLast = false;

    @Override
    public void init() {
        telemetry.addData("Status", "Initialized");

        leftDrive  = hardwareMap.get(DcMotor.class, "left_drive");
        rightDrive = hardwareMap.get(DcMotor.class, "right_drive");
        intakeDrive = hardwareMap.get(DcMotor.class, "intake_driver");
        shooter = hardwareMap.get(DcMotorEx.class, "shooter_motor");
        feeder = hardwareMap.get(Servo.class, "feeder");
        angle = hardwareMap.get(Servo.class, "angle");
        angleDegree = 0;
        feeder.setPosition(0);
        angle.setPosition(angleDegree);

        shooter.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        shooter.setMode(DcMotor.RunMode.RUN_USING_ENCODER);


        leftDrive.setDirection(DcMotor.Direction.FORWARD);
        rightDrive.setDirection(DcMotor.Direction.REVERSE);
        intakeDrive.setDirection(DcMotor.Direction.REVERSE);
        
        shooter.setVelocityPIDFCoefficients(8.0, 0.0, 0.0, 20.0);

        telemetry.addData("Status", "Initialized");
    }

    
    @Override
    public void init_loop() {
    }

    
    @Override
    public void start() {
        runtime.reset();
    }

    
    @Override
    public void loop() {

        //----------------------------------------driverbase
        double leftPower;
        double rightPower;

        double drive = -gamepad1.left_stick_y ;
        double turn  =  gamepad1.right_stick_x;
        leftPower    = Range.clip(drive + turn, -1.0, 1.0); 
        rightPower   = Range.clip(drive - turn, -1.0, 1.0);

        leftDrive.setPower(leftPower);
        rightDrive.setPower(rightPower);

        telemetry.addData("Status", "Run Time: " + runtime.toString());
        telemetry.addData("Motors", "left (%.2f), right (%.2f)", leftPower, rightPower);


        //----------------------------------------intake
        
        double intakePower = 0.0;

        if (gamepad1.a) {
            intakePower = 1.0;
        } else if (gamepad1.b) {
            intakePower = -1.0;
        }
        
        intakeDrive.setPower(intakePower);


        //----------------------------------------feeder
        double feederDegree = gamepad2.left_trigger;
        feederDegree = Range.clip(feederDegree, 0.0, 0.4);
        feeder.setPosition(feederDegree);
        telemetry.addData("feeder degree", feederDegree);
        

        //----------------------------------------angle    
        
        if (gamepad2.dpad_up && !dpadUpLast) {
            angleDegree += ANGLE_STEP;
        }
        else if (gamepad2.dpad_down && !dpadDownLast) {
            angleDegree -= ANGLE_STEP;
        }

        dpadUpLast = gamepad2.dpad_up;
        dpadDownLast = gamepad2.dpad_down;

        angleDegree = Range.clip(angleDegree, ANGLE_MIN, ANGLE_MAX);
        angle.setPosition(angleDegree);

        telemetry.addData("Angle Servo", angleDegree);

        
        //----------------------------------------Shooter

        double trigger = gamepad2.right_trigger;
        double TARGET_RPM = trigger * MAX_RPM;
        double targetTPS = (TARGET_RPM / 60.0) * TICKS_PER_REV * (15.0/10.0); 

        shooter.setVelocity(targetTPS);

        telemetry.addData("Target RPM", TARGET_RPM);
        telemetry.addData("Actual RPM", (shooter.getVelocity() / TICKS_PER_REV) * 60.0 * 1.5);
        telemetry.update();



    }

    @Override
    public void stop() {
    }

}
