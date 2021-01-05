package test;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    static final RobotType[] spawnableRobot = {
        RobotType.POLITICIAN,
        RobotType.SLANDERER,
        RobotType.MUCKRAKER,
    };

    static int turnCount;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;

        turnCount = 0;

        while (true) {
            turnCount += 1;
            switch (rc.getType()) {
                case ENLIGHTENMENT_CENTER: EnlightenmentCenter.run(); break;
                case POLITICIAN:           Politician.run();          break;
                case SLANDERER:            Slanderer.run();           break;
                case MUCKRAKER:            Muckraker.run();           break;
            }
            Clock.yield();
        }
    }
}
