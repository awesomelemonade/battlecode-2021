package ppbot;

import battlecode.common.*;
import ppbot.util.Cache;
import ppbot.util.Util;
import static ppbot.util.Constants.*;

public strictfp class Muckraker implements RunnableBot {
    private RobotController rc;

    private MapLocation cur_loc;
    private MapLocation spawnEC;

    private int initialExploreDirection;

    private int minX = -1, minY = -1, maxX = -1, maxY = -1;
    private int flag = 0;

    private MapLocation enemyEC;

    public Muckraker(RobotController rc) {
        this.rc = rc;
    }

    @Override
    public void init() throws GameActionException {

    }

    @Override
    public void turn() throws GameActionException {
        cur_loc = rc.getLocation();

        if (Cache.TURN_COUNT == 1) {
            spawnEC = Util.findSpawnEC();
            // determine which direction to explore by reading flag
            int flag_read = rc.getFlag(rc.senseRobotAtLocation(spawnEC).getID());
            System.out.println("Received flag: " + Integer.toString(flag_read));
            initialExploreDirection = flag_read & 7;
        }

        senseNearbyEnemyEC();

        if (rc.isReady()) {
            tryKillSlanderers();

            // Explore!
            MapLocation targetLocation = getExploreTarget();
            if (reachedExploreBorder()) {
                // If target loc is off the map, then we get a new explore location;
                initialExploreDirection = (initialExploreDirection + 1) & 7;
                targetLocation = getExploreTarget();
            }
            Util.tryMoveSmart(targetLocation);
        }

        rc.setFlag(flag);
    }

    private void senseNearbyEnemyEC() throws GameActionException {
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

    private void broadcastECLocation(MapLocation loc) {
        flag = 1 | (Util.packSigned128(loc.x - spawnEC.x) << 1) | (Util.packSigned128(loc.y - spawnEC.y) << 9);
    }

    private void tryKillSlanderers() throws GameActionException {
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

    private boolean reachedExploreBorder() throws GameActionException {
        int tempX = ORDINAL_OFFSET_X[initialExploreDirection];
        int tempY = ORDINAL_OFFSET_Y[initialExploreDirection];
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

    private MapLocation getExploreTarget() {
        return cur_loc.translate(ORDINAL_OFFSET_X[initialExploreDirection] * 64,
                ORDINAL_OFFSET_Y[initialExploreDirection] * 64);
    }

    private boolean greedyWalk(MapLocation loc) throws GameActionException {
        int least_dist = loc.distanceSquaredTo(cur_loc);
        int next = -1;
        for (int i = 0; i < ORDINAL_DIRECTIONS.length; i++) {
            MapLocation next_loc = cur_loc.add(ORDINAL_DIRECTIONS[i]);
            int temp_dist = next_loc.distanceSquaredTo(loc);
            if (temp_dist < least_dist && rc.canMove(ORDINAL_DIRECTIONS[i])) {
                least_dist = temp_dist;
                next = i;
            }
        }

        if (next != -1) {
            rc.move(ORDINAL_DIRECTIONS[next]);
            return true;
        }
        return false;
    }
}
