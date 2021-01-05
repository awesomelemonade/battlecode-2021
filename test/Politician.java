package test;

import battlecode.common.*;

import static test.RobotPlayer.rc;
import static test.Util.directions;
import static test.RobotPlayer.turnCount;
import static test.Util.packSigned128;
import static test.Util.unpackSigned128;

public class Politician {

    static MapLocation cur_loc;
    static MapLocation spawnEC;

    static MapLocation enemyEC = null;
    static boolean attack = false;

    public static void run() throws GameActionException {
        cur_loc = rc.getLocation();

        if (turnCount == 1) {
            spawnEC = Util.findSpawnEC();
            // determine which direction to explore by reading flag
            int flag_read = rc.getFlag(rc.senseRobotAtLocation(spawnEC).getID());
            parseECComms(flag_read);
        }

        if (attack) {
            tryAttack();
            greedyWalk(enemyEC);
        }
    }

    public static void tryAttack() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type == RobotType.ENLIGHTENMENT_CENTER
                    && cur_loc.distanceSquaredTo(robot.location) <= actionRadius) {
                // It's a slanderer... go get them!
                if (rc.canEmpower(actionRadius)) {
                    System.out.println("KILL!");
                    rc.empower(actionRadius);
                    return;
                }
            }
        }
    }

    public static void parseECComms(int f) {
        if ((f & 1) == 1) {
            enemyEC = new MapLocation(spawnEC.x + unpackSigned128(f >> 1), spawnEC.y + unpackSigned128(f >> 9));
            System.out.println("Politician received: " + enemyEC.toString());
            attack = true;
        }
    }

    public static void greedyWalk(MapLocation loc) throws GameActionException {
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

        if (next != -1)
            rc.move(directions[next]);
    }
}
