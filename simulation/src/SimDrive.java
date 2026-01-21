public class SimDrive implements DriveIO {
    public static class Pose {
        public double x;
        public double y;
        public double headingRad;
    }

    private enum Mode { IDLE, DRIVE, ROTATE, TANK }

    private final Pose pose = new Pose();
    private Mode mode = Mode.IDLE;
    private double remainingDistance = 0.0;
    private double driveSpeed = 0.0;
    private double remainingAngleRad = 0.0;
    private double rotateSpeed = 0.0;
    private double tankLeft = 0.0;
    private double tankRight = 0.0;
    private final double maxLinearSpeed = 40.0; // inches per second
    private final double trackWidth = 16.0;     // inches

    public Pose getPose() {
        return pose;
    }

    public void setPose(double xInches, double yInches, double headingRadians) {
        pose.x = xInches;
        pose.y = yInches;
        pose.headingRad = normalize(headingRadians);
        mode = Mode.IDLE;
        remainingDistance = 0.0;
        remainingAngleRad = 0.0;
        tankLeft = tankRight = 0.0;
    }

    public boolean isBusy() {
        return mode == Mode.DRIVE || mode == Mode.ROTATE;
    }

    @Override
    public void tank(double left, double right) {
        mode = Mode.TANK;
        tankLeft = left;
        tankRight = right;
    }

    @Override
    public void driveStraight(double inches, double power) {
        remainingDistance = inches;
        driveSpeed = Math.copySign(Math.abs(power) * maxLinearSpeed, inches);
        mode = Mode.DRIVE;
    }

    @Override
    public void rotate(double degrees, double power) {
        remainingAngleRad = Math.toRadians(-degrees);  // Reverse untuk FTC convention
        rotateSpeed = Math.copySign(Math.abs(power) * Math.toRadians(180.0), -degrees);
        mode = Mode.ROTATE;
    }

    @Override
    public void shoot(int count) {
        // no-op placeholder for simulation
    }

    @Override
    public void collect(int batch) {
        // no-op placeholder for simulation
    }

    public void update(double dtSeconds) {
        switch (mode) {
            case DRIVE:
                updateDrive(dtSeconds);
                break;
            case ROTATE:
                updateRotate(dtSeconds);
                break;
            case TANK:
                updateTank(dtSeconds);
                break;
            default:
                break;
        }
    }

    private void updateDrive(double dt) {
        double step = driveSpeed * dt;
        if (Math.abs(step) >= Math.abs(remainingDistance)) {
            step = remainingDistance;
            mode = Mode.IDLE;
        } else {
            remainingDistance -= step;
        }
        pose.x += step * Math.cos(pose.headingRad);
        pose.y += step * Math.sin(pose.headingRad);
    }

    private void updateRotate(double dt) {
        double step = rotateSpeed * dt;
        if (Math.abs(step) >= Math.abs(remainingAngleRad)) {
            step = remainingAngleRad;
            mode = Mode.IDLE;
        } else {
            remainingAngleRad -= step;
        }
        pose.headingRad = normalize(pose.headingRad + step);
    }

    private void updateTank(double dt) {
        double vL = tankLeft * maxLinearSpeed;
        double vR = tankRight * maxLinearSpeed;
        double linear = (vL + vR) / 2.0;
        double omega = (vR - vL) / trackWidth;
        pose.x += linear * dt * Math.cos(pose.headingRad);
        pose.y += linear * dt * Math.sin(pose.headingRad);
        pose.headingRad = normalize(pose.headingRad + omega * dt);
    }

    public void stop() {
        mode = Mode.IDLE;
        tankLeft = tankRight = 0.0;
    }

    private double normalize(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }
}
