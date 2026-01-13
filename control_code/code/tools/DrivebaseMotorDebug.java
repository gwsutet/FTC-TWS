package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.Range;

@TeleOp(name = "Drivebase Motor Debug", group = "Debug")
public class DrivebaseMotorDebug extends OpMode {

    DcMotorEx leftMotor;
    DcMotorEx rightMotor;

    // GANTI sesuai encoder motor drive kamu
    static final double TICKS_PER_REV = 28;  
    // goBILDA 5202 → 537.6

    @Override
    public void init() {

        leftMotor  = hardwareMap.get(DcMotorEx.class, "left_drive");
        rightMotor = hardwareMap.get(DcMotorEx.class, "right_drive");

        // coba dulu, nanti dibenerin pas debug
        leftMotor.setDirection(DcMotor.Direction.FORWARD);
        rightMotor.setDirection(DcMotor.Direction.REVERSE);

        leftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        leftMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        telemetry.addLine("Drivebase Debug READY");
        telemetry.update();
    }

    @Override
    public void loop() {

        // -------- DRIVER CONTROL (simple)
        double power = -gamepad1.left_stick_y;

        leftMotor.setPower(power);
        rightMotor.setPower(power);

        // -------- ENCODER DEBUG
        int leftPos  = leftMotor.getCurrentPosition();
        int rightPos = rightMotor.getCurrentPosition();

        double leftTPS  = leftMotor.getVelocity();
        double rightTPS = rightMotor.getVelocity();

        double leftRPM  = (leftTPS / TICKS_PER_REV) * 60.0;
        double rightRPM = (rightTPS / TICKS_PER_REV) * 60.0;

        // -------- TELEMETRY
        telemetry.addLine("=== DRIVEBASE MOTOR DEBUG ===");
        telemetry.addData("Power", "%.2f", power);

        telemetry.addLine("--- Encoder Position ---");
        telemetry.addData("Left  Pos", leftPos);
        telemetry.addData("Right Pos", rightPos);

        telemetry.addLine("--- Velocity ---");
        telemetry.addData("Left  TPS", "%.1f", leftTPS);
        telemetry.addData("Right TPS", "%.1f", rightTPS);

        telemetry.addLine("--- RPM ---");
        telemetry.addData("Left  RPM", "%.1f", leftRPM);
        telemetry.addData("Right RPM", "%.1f", rightRPM);

        telemetry.update();
    }

    @Override
    public void stop() {
        leftMotor.setPower(0);
        rightMotor.setPower(0);
    }
}
