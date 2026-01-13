public interface AprilTagIO {
    boolean isVisible();
    double yaw();    // radians
    double range();  // inches
}
