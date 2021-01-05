package latticebot.util;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Pathfinder {
    private static RobotController rc;
    public static void init(RobotController rc) {
        Pathfinder.rc = rc;
    }
    public static void execute(MapLocation target) throws GameActionException {
        if (rc.getLocation().equals(target)) {
            // already there
            return;
        }
        Util.tryMoveTowards(rc.getLocation().directionTo(target));
    }
}
