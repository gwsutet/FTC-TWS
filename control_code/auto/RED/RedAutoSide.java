case IDLE:
                angle.setPosition(0.4);
                feeder.setPosition(0.0);
                driveStraight(0.5, -60);    // Mundur kencang
                rotateExactFast(-40, 0.6); // Miring kiri awal
                currentState = AutoState.SCAN_CENTER;
                break;