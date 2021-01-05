package ppbot.util;

import battlecode.common.*;
import ppbot.util.Cache;
import static ppbot.util.Constants.*;

public class Util {
    private static RobotController rc;

    public static void init(RobotController rc) {
        Util.rc = rc;
        Constants.init(rc);
        Cache.init(rc);
        MapInfo.init(rc);
    }

    public static void loop() {
        Cache.loop();
        MapInfo.loop();
    }

    public static RobotType randomSpawnableRobotType() {
        return SPAWNABLE_ROBOTS[(int) (Math.random() * SPAWNABLE_ROBOTS.length)];
    }

    public static boolean tryBuildRobot(RobotType type, Direction direction, int influence) throws GameActionException {
        if (rc.canBuildRobot(type, direction, influence)) {
            rc.buildRobot(type, direction, influence);
            return true;
        } else {
            return false;
        }
    }

    public static boolean tryBuildRobotTowards(RobotType type, Direction direction, int influence)
            throws GameActionException {
        for (Direction buildDirection : Constants.getAttemptOrder(direction)) {
            if (tryBuildRobot(type, buildDirection, influence)) {
                return true;
            }
        }
        return false;
    }

    public static boolean tryMove(Direction direction) throws GameActionException {
        if (rc.canMove(direction)) {
            rc.move(direction);
            return true;
        } else {
            return false;
        }
    }

    public static boolean tryMoveTowards(Direction direction) throws GameActionException {
        for (Direction moveDirection : Constants.getAttemptOrder(direction)) {
            if (tryMove(moveDirection)) {
                return true;
            }
        }
        return false;
    }

    public static boolean tryRandomMove() throws GameActionException {
        return tryMove(randomAdjacentDirection());
    }

    /**
     * Usage: Util.random(Constants.CARDINAL_DIRECTIONS)
     * 
     * @param directions
     * @return
     */
    public static Direction random(Direction[] directions) {
        return directions[(int) (Math.random() * directions.length)];
    }

    public static Direction randomAdjacentDirection() {
        return random(ORDINAL_DIRECTIONS);
    }

    public static MapLocation findSpawnEC() throws GameActionException {
        for (RobotInfo r : Cache.ALLY_ROBOTS) {
            if (r.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                return r.getLocation();
            }
        }
        return null;
    }

    public static int packSigned128(int value) { // 8 bit
        if (value < 0) {
            return 128 - value;
        }
        return value;
    }

    public static int unpackSigned128(int value) { // 8 bit
        if ((value & 255) >= 128) {
            return 128 - (value & 255);
        }
        return (value & 255);
    }
}
