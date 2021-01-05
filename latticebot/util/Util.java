package latticebot.util;

import battlecode.common.*;

import static ppbot.util.Constants.ORDINAL_DIRECTIONS;
import static ppbot.util.Constants.SPAWNABLE_ROBOTS;

public class Util {
    private static RobotController rc;

    public static void init(RobotController rc) {
        Util.rc = rc;
        Constants.init(rc);
        Cache.init(rc);
        MapInfo.init(rc);
        LatticeUtil.init(rc);
        Pathfinder.init(rc);
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
    private static Direction previousDirection = randomAdjacentDirection();
    public static boolean randomExplore() throws GameActionException {
        if (tryMove(previousDirection)) {
            return true;
        }
        previousDirection = randomAdjacentDirection();
        return false;
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

    public static RobotInfo getClosestEnemyRobotWithin(int limit) {
        int bestDistanceSquared = limit + 1;
        RobotInfo bestRobot = null;
        for (RobotInfo enemy : Cache.ENEMY_ROBOTS) { // TODO: Tie breakers, use communication
            int distanceSquared = enemy.getLocation().distanceSquaredTo(rc.getLocation());
            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                bestRobot = enemy;
            }
        }
        return bestRobot;
    }
    public static RobotInfo getClosestEnemyRobot() {
        return getClosestEnemyRobotWithin(Constants.MAX_DISTANCE_SQUARED);
    }
}
