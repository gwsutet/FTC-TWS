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
    private final double orientationRad;  // tambah orientasi tag
    private final AtomicBoolean visible = new AtomicBoolean(true);
    
    // Camera FOV and range limits for realistic simulation (Logitech C270)
    private static final double CAMERA_FOV_RAD = Math.toRadians(55.0);  // 55 degree FOV (C270 actual)
    private static final double MAX_RANGE_INCH = 120.0;  // 120 inch max detection (C270 limited)
    private static final double MAX_YAW_RAD = Math.toRadians(27.5);  // 27.5 degree max yaw (half FOV)
    private static final double CAMERA_HEIGHT_INCH = 16.0;  // 16 inch height

    // Measured values updated each tick
    private volatile double measuredYawRad = 0.0;   // relative to robot heading
    private volatile double measuredRangeIn = 0.0;
    private volatile TagPose lastPose = new TagPose(0, 0, 0, 0, false);

    // Optional bias knobs to simulate sensor offset without breaking yaw math
    private volatile double yawBiasRad = 0.0;

    public SimAprilTag(int id, double fieldXInches, double fieldYInches) {
        this(id, fieldXInches, fieldYInches, 0.0);
    }
    
    public SimAprilTag(int id, double fieldXInches, double fieldYInches, double orientationRadians) {
        this.id = id;
        this.fieldX = fieldXInches;
        this.fieldY = fieldYInches;
        this.orientationRad = orientationRadians;
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
    
    public double getOrientation() {
        return orientationRad;
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
     * Compute current tag measurement relative to robot pose with realistic camera limitations.
     */
    public TagPose computePose(double robotX, double robotY, double robotHeadingRad) {
        double dx = fieldX - robotX;
        double dy = fieldY - robotY;
        double range = Math.hypot(dx, dy);
        double absoluteBearing = Math.atan2(dy, dx);
        double yawRel = normalize(absoluteBearing - robotHeadingRad + yawBiasRad);
        
        // Apply realistic camera limitations with height compensation
        // AprilTag height in FTC CenterStage: 23 inches from floor (pylon top)
        double tagHeight = 23.0;
        double heightDiff = tagHeight - CAMERA_HEIGHT_INCH;
        double horizontalDistance = range;
        double verticalDistance = heightDiff;
        
        // Calculate pitch angle (vertical angle from camera to tag)
        double pitchAngle = Math.atan2(verticalDistance, horizontalDistance);
        
        // Camera vertical FOV limit (typically 30-40 degrees total)
        double maxPitchAngle = Math.toRadians(20.0);  // 20 degrees up from horizontal
        
        // Calculate actual 3D distance for range checking
        double adjustedRange = Math.sqrt(horizontalDistance * horizontalDistance + verticalDistance * verticalDistance);
        
        // Check visibility with pitch angle constraint
        boolean vis = visible.get() && 
                     adjustedRange <= MAX_RANGE_INCH && 
                     Math.abs(yawRel) <= MAX_YAW_RAD &&
                     pitchAngle <= maxPitchAngle;  // Must be able to look up at the tag

        measuredRangeIn = range;  // Keep floor distance for driving
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
