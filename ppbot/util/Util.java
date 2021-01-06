package ppbot.util;

import java.util.*;

import battlecode.common.*;
import ppbot.util.Cache;
import static ppbot.util.Constants.*;
import ppbot.util.Pair;

public class Util {
    private static RobotController rc;

    public static void init(RobotController rc) throws GameActionException {
        Util.rc = rc;
        Constants.init(rc);
        Cache.init(rc);
        MapInfo.init(rc);
    }

    public static void loop() throws GameActionException {
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

    public static int squaredEuclidLength(int dx, int dy) {
        return dx * dx + dy * dy;
    }

    public static int squaredEuclidDistance(int x1, int y1, int x2, int y2) {
        return squaredEuclidLength(x1 - x2, y1 - y2);
    }

    public static int moveLength(int dx, int dy) {
        return Math.abs(dx) + Math.abs(dy);
    }

    public static int moveDistance(int x1, int y1, int x2, int y2) {
        return moveLength(x1 - x2, y1 - y2);
    }

    public static int moveDistance(MapLocation loc1, MapLocation loc2) {
        return moveDistance(loc1.x, loc1.y, loc2.x, loc2.y);
    }

    public static Direction offsetToDirection(int dx, int dy) throws GameActionException {
        switch (3 * (dx + 1) + (dy + 1)) {
            case 0:
                return Direction.SOUTHWEST;
            case 1:
                return Direction.WEST;
            case 2:
                return Direction.NORTHWEST;
            case 3:
                return Direction.SOUTH;
            case 4:
                return Direction.CENTER;
            case 5:
                return Direction.NORTH;
            case 6:
                return Direction.SOUTHEAST;
            case 7:
                return Direction.EAST;
            case 8:
                return Direction.NORTHEAST;
        }
        return null;
    }

    public static boolean tryMoveSmart(MapLocation dest)
            throws GameActionException {
        if (Cache.MY_LOCATION.equals(dest)) {
            return false;
        }

        double lowest_wait = 1e9;
        int cur_dis_euclid = Cache.MY_LOCATION.distanceSquaredTo(dest);
        int cur_dis_move = moveDistance(Cache.MY_LOCATION, dest);
        Direction best_dir = null;
        for (Direction dir : ORDINAL_DIRECTIONS) if (rc.canMove(dir)) {
            MapLocation nextpos = Cache.MY_LOCATION.add(dir);
            if (nextpos.distanceSquaredTo(dest) >= cur_dis_euclid && moveDistance(nextpos, dest) >= cur_dis_move) {
                continue;
            }

            double wait_time = 1.0 / rc.sensePassability(nextpos);
            if (wait_time < lowest_wait) {
                lowest_wait = wait_time;
                best_dir = dir;
            }
        }
        if(best_dir == null) {
            return false;
        }
        return tryMove(best_dir);
    }

    public static boolean tryMoveAwaySmart(MapLocation loc) throws GameActionException {
        int cur_x = Cache.MY_LOCATION.x;
        int cur_y = Cache.MY_LOCATION.y;
        MapLocation dest = new MapLocation(2*cur_x - loc.x, 2*cur_y - loc.y);
        return tryMoveSmart(dest);
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
