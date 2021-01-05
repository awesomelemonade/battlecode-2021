package test;

import battlecode.common.*;

import static test.RobotPlayer.rc;
import static test.Util.directions;
import static test.Util.directionOffX;
import static test.Util.directionOffY;
import static test.RobotPlayer.turnCount;
import static test.Util.packSigned128;
import static test.Util.unpackSigned128;

public class Muckraker {

    static MapLocation cur_loc;
    static MapLocation spawnEC;

    static int initialExploreDirection;

    static int minX = -1, minY = -1, maxX = -1, maxY = -1;
    static int flag = 0;

    static MapLocation enemyEC;

    public static void run() throws GameActionException {
        cur_loc = rc.getLocation();

        if (turnCount == 1) {
            spawnEC = Util.findSpawnEC();
            // determine which direction to explore by reading flag
            int flag_read = rc.getFlag(rc.senseRobotAtLocation(spawnEC).getID());
            System.out.println("Received flag: " + Integer.toString(flag_read));
            initialExploreDirection = flag_read & 7;
        }

        senseNearbyEnemyEC();

        if (rc.isReady()) {
            tryKillSlanderers();
        }

        if (rc.isReady()) {
            // Explore!
            MapLocation targetLocation = getExploreTarget();
            if (reachedExploreBorder()) {
                // If target loc is off the map, then we get a new explore location;
                initialExploreDirection = (initialExploreDirection + 1) & 7;
                targetLocation = getExploreTarget();
            }
            greedyWalk(targetLocation);
        }

        rc.setFlag(flag);
    }

    public static void senseNearbyEnemyEC() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                enemyEC = robot.location;
                System.out
                        .println("Found enemy EC: " + Integer.toString(enemyEC.x) + " " + Integer.toString(enemyEC.y));
                broadcastECLocation(enemyEC);
            }
        }
    }

    public static void broadcastECLocation(MapLocation loc) {
        flag = 1 | (packSigned128(loc.x - spawnEC.x) << 1) | (packSigned128(loc.y - spawnEC.y) << 9);
    }

    public static void tryKillSlanderers() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
                    System.out.println("e x p o s e d");
                    rc.expose(robot.location);
                    return;
                } else {
                    greedyWalk(robot.location);
                }
            }
        }
    }

    public static boolean reachedExploreBorder() throws GameActionException {
        int tempX = directionOffX[initialExploreDirection];
        int tempY = directionOffY[initialExploreDirection];
        MapLocation loc1 = cur_loc.translate(tempX * 2, 0);
        MapLocation loc2 = cur_loc.translate(0, tempY * 2);
        if (tempX != 0 && rc.canDetectLocation(loc1) && rc.onTheMap(loc1)) {
            return false;
        }
        if (tempY != 0 && rc.canDetectLocation(loc2) && rc.onTheMap(loc2)) {
            return false;
        }
        return true;
    }

    public static MapLocation getExploreTarget() {
        return cur_loc.translate(directionOffX[initialExploreDirection] * 64,
                directionOffY[initialExploreDirection] * 64);
    }

    public static boolean greedyWalk(MapLocation loc) throws GameActionException {
        int least_dist = loc.distanceSquaredTo(cur_loc);
        int next = -1;
        for (int i = 0; i < directions.length; i++) {
            MapLocation next_loc = cur_loc.add(directions[i]);
            int temp_dist = next_loc.distanceSquaredTo(loc);
            if (temp_dist < least_dist && rc.canMove(directions[i])) {
                least_dist = temp_dist;
                next = i;
            }
        }

        if (next != -1) {
            rc.move(directions[next]);
            return true;
        }
        return false;
    }
}
