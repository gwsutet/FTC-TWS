import java.util.concurrent.atomic.AtomicBoolean;

public class SimAprilTag implements AprilTagIO {
    public static class TagPose {
        public final double x;
        public final double y;
        public final double yaw;
        public final double range;
        public final boolean visible;

        public TagPose(double x, double y, double yaw, double range, boolean visible) {
            this.x = x;
            this.y = y;
            this.yaw = yaw;
            this.range = range;
            this.visible = visible;
        }
    }

    private final int id;
    private final double fieldX;
    private final double fieldY;
    private final AtomicBoolean visible = new AtomicBoolean(true);

    // Measured values updated each tick
    private volatile double measuredYawRad = 0.0;   // relative to robot heading
    private volatile double measuredRangeIn = 0.0;
    private volatile TagPose lastPose = new TagPose(0, 0, 0, 0, false);

    // Optional bias knobs to simulate sensor offset without breaking yaw math
    private volatile double yawBiasRad = 0.0;

    public SimAprilTag(int id, double fieldXInches, double fieldYInches) {
        this.id = id;
        this.fieldX = fieldXInches;
        this.fieldY = fieldYInches;
    }

    public int getId() {
        return id;
    }

    public double getFieldX() {
        return fieldX;
    }

    public double getFieldY() {
        return fieldY;
    }

    public void setVisible(boolean v) {
        visible.set(v);
    }

    public void toggleVisible() {
        visible.set(!visible.get());
    }

    /**
     * Adjusts a bias to yaw measurement (simulates calibration error) without removing yaw support.
     */
    public void adjustYawBias(double delta) {
        this.yawBiasRad += delta;
    }

    public void resetYawBias() {
        this.yawBiasRad = 0.0;
    }

    /**
     * Backwards-compat helpers for existing keybindings; they adjust yaw bias only.
     */
    public void adjustYawRadians(double delta) {
        adjustYawBias(delta);
    }

    public void adjustRangeInches(double delta) {
        // Range is geometry-derived; keep method for compatibility but no-op.
    }

    /**
     * Compute current tag measurement relative to robot pose.
     */
    public TagPose computePose(double robotX, double robotY, double robotHeadingRad) {
        double dx = fieldX - robotX;
        double dy = fieldY - robotY;
        double range = Math.hypot(dx, dy);
        double absoluteBearing = Math.atan2(dy, dx);
        double yawRel = normalize(absoluteBearing - robotHeadingRad + yawBiasRad);
        boolean vis = visible.get();

        measuredRangeIn = range;
        measuredYawRad = yawRel;
        lastPose = new TagPose(fieldX, fieldY, yawRel, range, vis);
        return lastPose;
    }

    private double normalize(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    @Override
    public boolean isVisible() {
        return visible.get();
    }

    @Override
    public double yaw() {
        return measuredYawRad;
    }

    @Override
    public double range() {
        return measuredRangeIn;
    }

    public TagPose getLastPose() {
        return lastPose;
    }
}
