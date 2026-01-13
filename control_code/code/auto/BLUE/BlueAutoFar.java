case IDLE:
                angle.setPosition(0.4);
                feeder.setPosition(0.0);
                rotateExactFast(40, 0.6); // Miring kiri awal
                driveStraight(0.5, 40);    // Maju kencang
                currentState = AutoState.SCAN_CENTER;
                break;