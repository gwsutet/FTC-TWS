public class AutoLogic {
    public enum AutoState { IDLE, SCAN_CENTER, SEARCH_SHOOTER, LOCK_SHOOTER, TURN_BACK, SHOOTING, COLLECT_BALLS }

    private AutoState currentState = AutoState.IDLE;
    private int ballBatchCount = 0;
    private boolean hasStarted = false;

    private boolean actionActive = false;
    private double actionTimer = 0.0;
    private Runnable onActionComplete = null;

    private final double linearSpeedInPerSec = 40.0;  // simulated max speed
    private final double angularSpeedDegPerSec = 180.0;
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
    }

    public void update(double dtSeconds, AprilTagIO tag21, AprilTagIO tag24, DriveIO drive) {
        if (actionActive) {
            actionTimer -= dtSeconds;
            if (actionTimer <= 0) {
                actionActive = false;
                if (onActionComplete != null) onActionComplete.run();
            }
            return;
        }

        switch (currentState) {
            case IDLE:
                if (!hasStarted) {
                    hasStarted = true;
                    startInitialSequence(drive);
                }
                break;

            case SCAN_CENTER:
                if (tag21 != null && tag21.isVisible()) {
                    drive.tank(0, 0);
                    double rangeError = tag21.range() - 65.0;
                    if (Math.abs(rangeError) > distanceTolerance) {
                        startDriveAction(rangeError, 0.4, drive, null);
                    } else {
                        currentState = AutoState.SEARCH_SHOOTER;
                    }
                } else {
                    drive.tank(-0.18, 0.18);
                }
                break;

            case SEARCH_SHOOTER:
                if (tag24 != null && tag24.isVisible()) {
                    drive.tank(0, 0);
                    currentState = AutoState.LOCK_SHOOTER;
                } else {
                    drive.tank(0.16, -0.16);
                }
                break;

            case LOCK_SHOOTER:
                if (tag24 != null && tag24.isVisible()) {
                    double rangeError = tag24.range() - 68.0;
                    if (Math.abs(rangeError) > distanceTolerance) {
                        startDriveAction(rangeError, 0.6, drive, null);
                    } else {
                        currentState = AutoState.TURN_BACK;
                    }
                } else {
                    currentState = AutoState.SEARCH_SHOOTER;
                }
                break;

            case TURN_BACK:
                startRotateAction(270.0, 0.6, drive, () -> currentState = AutoState.SHOOTING);
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
        startRotateAction(-40.0, 0.6, drive,
                () -> startDriveAction(40.0, 0.5, drive,
                        () -> currentState = AutoState.SCAN_CENTER));
    }

    private void startCollectSequence(DriveIO drive) {
        ballBatchCount++;
        double driveDist = 30.0;
        if (ballBatchCount == 2) driveDist = 50.0;
        if (ballBatchCount >= 3) driveDist = 80.0;

        final double distFinal = driveDist;
        Runnable step6 = () -> startRotateAction(-120.0, 0.6, drive, () -> currentState = AutoState.SEARCH_SHOOTER);
        Runnable step5 = () -> startDriveAction(-50.0, 0.5, drive, step6);
        Runnable step4 = () -> {
            drive.collect(ballBatchCount);
            startDriveAction(50.0, 0.3, drive, step5);
        };
        Runnable step3 = () -> startRotateAction(-120.0, 0.6, drive, step4);
        Runnable step2 = () -> startDriveAction(distFinal, 0.3, drive, step3);
        Runnable step1 = () -> startRotateAction(-70.0, 0.6, drive, step2);

        startDriveAction(20.0, 0.3, drive, step1);
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
