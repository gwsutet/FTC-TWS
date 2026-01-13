public interface DriveIO {
    void tank(double left, double right);
    void driveStraight(double inches, double power);
    void rotate(double degrees, double power);
    void shoot(int count);
    void collect(int batch);
}
