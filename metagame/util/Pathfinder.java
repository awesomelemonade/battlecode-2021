package metagame.util;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Pathfinder {
    private static RobotController rc;
    public static void init(RobotController rc) {
        Pathfinder.rc = rc;
    }
    public static int moveDistance(MapLocation a, MapLocation b) {
        return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
    }
    public static boolean execute(MapLocation target) throws GameActionException {
        if (rc.getLocation().equals(target)) {
            // already there
            return true;
        }
        // Out of all possible moves that lead to a lower euclidean distance OR lower move distance,
        // find the direction that goes to the highest passability
        // euclidean distance defined by dx^2 + dy^2
        // move distance defined by max(dx, dy)
        // ties broken by "preferred direction" dictated by Constants.getAttemptOrder
        double highestPassability = 0;
        int targetDistanceSquared = rc.getLocation().distanceSquaredTo(target) - 1; // suubtract 1 to be strictly less
        int targetMoveDistance = moveDistance(rc.getLocation(), target);
        Direction bestDirection = null;
        for (Direction direction : Constants.getAttemptOrder(rc.getLocation().directionTo(target))) {
            if (rc.canMove(direction)) {
                MapLocation location = rc.getLocation().add(direction);
                if (location.isWithinDistanceSquared(target, targetDistanceSquared) || moveDistance(location, target) < targetMoveDistance) {
                    double passability = rc.sensePassability(location);
                    if (passability > highestPassability) {
                        highestPassability = passability;
                        bestDirection = direction;
                    }
                }
            }
        }
        if (bestDirection != null) {
            rc.move(bestDirection);
            return true;
        }
        return false;
    }
}
