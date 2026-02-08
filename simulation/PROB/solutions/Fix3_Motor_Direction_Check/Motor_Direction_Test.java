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
import java.util.concurrent.TimeUnit;

@Autonomous(name = "Motor Direction Test", group = "Diagnostics")
public class Motor_Direction_Test extends LinearOpMode {

    private DcMotor leftDrive, rightDrive;

    @Override
    public void runOpMode() {
        initHardware();
        
        telemetry.addLine("=================================");
        telemetry.addLine("MOTOR DIRECTION TEST");
        telemetry.addLine("=================================");
        telemetry.addLine("This will test each motor");
        telemetry.addLine("individually to verify");
        telemetry.addLine("forward/reverse direction.");
        telemetry.addLine("");
        telemetry.addLine("WATCH THE WHEELS:");
        telemetry.addLine("- LEFT motor should move");
        telemetry.addLine("  FORWARD for 2 sec");
        telemetry.addLine("- RIGHT motor should move");
        telemetry.addLine("  FORWARD for 2 sec");
        telemetry.addLine("");
        telemetry.addLine("Press PLAY to start test");
        telemetry.update();
        
        waitForStart();

        // Test LEFT motor FORWARD
        telemetry.clear();
        telemetry.addLine(">>> TESTING LEFT MOTOR FORWARD");
        telemetry.addLine("Left wheel should spin FORWARD");
        telemetry.update();
        leftDrive.setPower(0.3);
        sleep(2000);
        leftDrive.setPower(0);
        sleep(500);
        
        // Test RIGHT motor FORWARD
        telemetry.clear();
        telemetry.addLine(">>> TESTING RIGHT MOTOR FORWARD");
        telemetry.addLine("Right wheel should spin FORWARD");
        telemetry.update();
        rightDrive.setPower(0.3);
        sleep(2000);
        rightDrive.setPower(0);
        sleep(500);
        
        // Test both FORWARD (robot should move forward)
        telemetry.clear();
        telemetry.addLine(">>> TESTING BOTH FORWARD");
        telemetry.addLine("Robot should move FORWARD");
        telemetry.update();
        leftDrive.setPower(0.3);
        rightDrive.setPower(0.3);
        sleep(2000);
        leftDrive.setPower(0);
        rightDrive.setPower(0);
        sleep(500);
        
        // Test turn LEFT (left back, right forward)
        telemetry.clear();
        telemetry.addLine(">>> TESTING TURN LEFT");
        telemetry.addLine("Left wheel BACK, Right wheel FORWARD");
        telemetry.update();
        leftDrive.setPower(-0.3);
        rightDrive.setPower(0.3);
        sleep(2000);
        leftDrive.setPower(0);
        rightDrive.setPower(0);
        
        telemetry.clear();
        telemetry.addLine("=================================");
        telemetry.addLine("TEST COMPLETE");
        telemetry.addLine("=================================");
        telemetry.addLine("Check if movements matched");
        telemetry.addLine("expected behavior.");
        telemetry.addLine("");
        telemetry.addLine("If robot spun in circles during");
        telemetry.addLine("turn test, motor directions");
        telemetry.addLine("need to be swapped.");
        telemetry.update();
        
        sleep(5000);
    }

    void initHardware() {
        leftDrive = hardwareMap.get(DcMotor.class, "left_drive");
        rightDrive = hardwareMap.get(DcMotor.class, "right_drive");
        
        // Same as teleop
        leftDrive.setDirection(DcMotor.Direction.FORWARD);
        rightDrive.setDirection(DcMotor.Direction.REVERSE);
        
        leftDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        leftDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }
}
