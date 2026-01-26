public class AutoLogic {
    public enum AutoState { IDLE, SCAN_CENTER, SEARCH_SHOOTER, LOCK_SHOOTER, TURN_BACK, SHOOTING, COLLECT_BALLS }

    private AutoState currentState = AutoState.IDLE;
    private int ballBatchCount = 0;
    private boolean hasStarted = false;

    private boolean actionActive = false;
    private double actionTimer = 0.0;
    private Runnable onActionComplete = null;
    private boolean sleepActive = false;
    private double sleepTimer = 0.0;
    private Runnable onSleepComplete = null;

    // Dynamic shooting target based on scan
    private AprilTagIO shootingTarget = null;

    // Initial position settings
    private double initialX = 5.0;  // Starting X position
    private double initialY = -55.0;  // Starting Y position
    private double initialHeading = 45.0;  // Starting heading in degrees
    private boolean useInitialPosition = false;  // Toggle for initial position

    private final double linearSpeedInPerSec = 100.0;  // simulated max speed
    private final double angularSpeedDegPerSec = 360.0;
    private final double distanceTolerance = 1.0;

    public AutoState getCurrentState() {
        return currentState;
    }

    public void reset() {
        currentState = AutoState.IDLE;
        ballBatchCount = 0;
        hasStarted = false;
        actionActive = false;
        actionTimer = 0.0;
        onActionComplete = null;
        sleepActive = false;
        sleepTimer = 0.0;
        onSleepComplete = null;
        shootingTarget = null;
    }

    // Set initial position for simulation
    public void setInitialPosition(double x, double y, double heading) {
        this.initialX = x;
        this.initialY = y;
        this.initialHeading = heading;
        this.useInitialPosition = true;
    }

    // Enable/disable initial position
    public void setUseInitialPosition(boolean use) {
        this.useInitialPosition = use;
    }

    // Get initial position info
    public double getInitialX() { return initialX; }
    public double getInitialY() { return initialY; }
    public double getInitialHeading() { return initialHeading; }
    public boolean getUseInitialPosition() { return useInitialPosition; }

    // Set position if DriveIO supports it
    private void setRobotPosition(DriveIO drive) {
        if (useInitialPosition && drive instanceof SimDrive) {
            SimDrive simDrive = (SimDrive) drive;
            simDrive.setPose(initialX, initialY, Math.toRadians(initialHeading));
        }
    }

    public void update(double dtSeconds, AprilTagIO tag21, AprilTagIO tag20, AprilTagIO tag24, DriveIO drive) {
        if (actionActive) {
            actionTimer -= dtSeconds;
            if (actionTimer <= 0) {
                actionActive = false;
                if (onActionComplete != null) onActionComplete.run();
            }
            return;
        }

        if (sleepActive) {
            sleepTimer -= dtSeconds;
            // Stop semua movement saat sleep
            drive.tank(0, 0);
            if (sleepTimer <= 0) {
                sleepActive = false;
                if (onSleepComplete != null) {
                    onSleepComplete.run();
                    onSleepComplete = null; // Clear setelah dipanggil
                }
            }
            return;
        }

        switch (currentState) {
            case IDLE:
                if (!hasStarted) {
                    hasStarted = true;
                    setRobotPosition(drive);  // Set initial position
                    startInitialSequence(drive);
                }
                break;

            case SCAN_CENTER:
                if (tag21 != null && tag21.isVisible()) {
                    // Ngehadap ke tag21 dulu
                    double yawError = tag21.yaw();
                    if (Math.abs(yawError) > Math.toRadians(5)) {
                        drive.tank(0.25, -0.25);  // Putar clockwise (kanan) untuk ngehadap tag
                    } else {
                        drive.tank(0, 0);  // Stop kalo udah ngehadek
                        double rangeError = tag21.range() - 60.0;  // Minimal 24 inch dari tag21
                        if (Math.abs(rangeError) > distanceTolerance) {
                            startDriveAction(rangeError, 0.9, drive, null);
                        } else {
                            currentState = AutoState.SEARCH_SHOOTER;
                        }
                    }
                } else {
                    drive.tank(0.18, -0.18);  // Putar clockwise (kanan) cari tag
                }
                break;

            case SEARCH_SHOOTER:
                // Cari tag20 atau tag24, yang pertama ketemu itu jadi target
                if (tag20 != null && tag20.isVisible()) {
                    shootingTarget = tag20;
                    System.out.println("BLUE Found tag20, using as shooting target");
                } else if (tag24 != null && tag24.isVisible()) {
                    shootingTarget = tag24;
                    System.out.println("BLUE Found tag24, using as shooting target");
                }
                
                if (shootingTarget != null && shootingTarget.isVisible()) {
                    // Ngehadap ke target yang ditemukan
                    double yawError = shootingTarget.yaw();
                    if (Math.abs(yawError) > Math.toRadians(5)) {
                        drive.tank(-0.3, 0.3);  // Putar clockwise (kanan) untuk ngehadap tag
                    } else {
                        drive.tank(0, 0);  // Stop kalo udah ngehadek
                        currentState = AutoState.LOCK_SHOOTER;
                    }
                } else {
                    drive.tank(-0.18, 0.18);  // Putar counter-clockwise (kiri) cari tag
                }
                break;

            case LOCK_SHOOTER:
                if (shootingTarget != null && shootingTarget.isVisible()) {
                    // Cek yaw alignment dulu
                    double yawError = shootingTarget.yaw();
                    if (Math.abs(yawError) > Math.toRadians(3)) {
                        drive.tank(-0.25, 0.25);  // Putar clockwise (kanan) halus untuk alignment
                    } else {
                        // Udah ngehadek, cek range
                        double rangeError = shootingTarget.range() - 68.0;
                        if (Math.abs(rangeError) > distanceTolerance) {
                            startDriveAction(rangeError, 0.9, drive, null);
                        } else {
                            System.out.println("BLUE LOCK_SHOOTER -> TURN_BACK (range OK)");
                            currentState = AutoState.TURN_BACK;
                        }
                    }
                } else {
                    System.out.println("BLUE LOCK_SHOOTER -> SEARCH_SHOOTER (target lost)");
                    shootingTarget = null;  // Reset target
                    currentState = AutoState.SEARCH_SHOOTER;
                }
                break;

            case TURN_BACK:
                startRotateAction(180.0, 0.9, drive, () -> currentState = AutoState.SHOOTING);
                break;

            case SHOOTING:
                startShootAction(drive, () -> currentState = AutoState.COLLECT_BALLS);
                break;

            case COLLECT_BALLS:
                startCollectSequence(drive);
                break;
        }
    }

    private void startInitialSequence(DriveIO drive) {
        startRotateAction(60.0, 1.0, drive,
        () -> startDriveAction(-80.0, 0.9, drive, 
         () -> startRotateAction(90.0, 1.0, drive, 
            () -> startDriveAction(-90.0, 0.9, drive, 
                () -> currentState = AutoState.SCAN_CENTER))));

        // startRotateAction(-40.0, 1.0, drive,
        //         () -> startDriveAction(40.0, 0.9, drive,
        //                 () -> currentState = AutoState.SCAN_CENTER));
    }

    private void startCollectSequence(DriveIO drive) {
        ballBatchCount++;
        double driveDist = 55.0;
        // if (ballBatchCount == 2) driveDist = 50.0;
        // if (ballBatchCount >= 3) driveDist = 80.0;

        final double distFinal = driveDist;
        Runnable step8 = () -> startRotateAction(-45.0, 1.0, drive, () -> currentState = AutoState.SEARCH_SHOOTER);
        Runnable step7 = () -> startDriveAction(distFinal, 0.8, drive, step8);
        Runnable step6 = () -> startRotateAction(180.0, 1.0, drive, step7);
        Runnable step5 = () -> startDriveAction(-120.0, 0.8, drive, step6);
        Runnable step4 = () -> {
            drive.collect(ballBatchCount);
            // Maju cepat 50 inch dulu
            startDriveAction(50.0, 0.8, drive, () -> {
                // Lalu maju lambat 30 inch
                startDriveAction(70.0, 0.3, drive, () -> {
                    // Setelah drive selesai, mulai sleep 1 detik
                    beginSleep(1.0, step5);
                });
            });
        };
        Runnable step3 = () -> startRotateAction(180.0, 1.0, drive, step4);
        Runnable step2 = () -> startDriveAction(distFinal, 0.8, drive, step3);
        Runnable step1 = () -> startRotateAction(79.2, 1.0, drive, step2);

        startDriveAction(0.0, 0.3, drive, step1);
    }

    private void startDriveAction(double inches, double power, DriveIO drive, Runnable onDone) {
        double speed = Math.max(0.1, Math.abs(power)) * linearSpeedInPerSec;
        double duration = Math.abs(inches) / speed;
        drive.driveStraight(inches, power);
        beginAction(duration, onDone);
    }

    private void startRotateAction(double degrees, double power, DriveIO drive, Runnable onDone) {
        double speed = Math.max(0.1, Math.abs(power)) * angularSpeedDegPerSec;
        double duration = Math.abs(degrees) / speed;
        drive.rotate(degrees, power);
        beginAction(duration, onDone);
    }

    private void startShootAction(DriveIO drive, Runnable onDone) {
        drive.shoot(3);
        beginAction(2.0, onDone);
    }

    private void beginSleep(double durationSeconds, Runnable onDone) {
        if (durationSeconds <= 0) {
            if (onDone != null) onDone.run();
            return;
        }
        sleepActive = true;
        sleepTimer = durationSeconds;
        onSleepComplete = onDone;
    }

    private void beginAction(double durationSeconds, Runnable onDone) {
        if (durationSeconds <= 0) {
            if (onDone != null) onDone.run();
            return;
        }
        actionActive = true;
        actionTimer = durationSeconds;
        onActionComplete = onDone;
    }
}