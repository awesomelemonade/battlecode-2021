package ppbot;

import battlecode.common.*;
import ppbot.util.Cache;
import ppbot.util.Util;
import static ppbot.util.Constants.*;

public strictfp class Politician implements RunnableBot {
    private RobotController rc;

    private MapLocation cur_loc;
    private MapLocation spawnEC;

    private MapLocation enemyEC = null;
    private boolean attack = false;

    public Politician(RobotController rc) {
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
            parseECComms(flag_read);
        }

        if (attack) {
            tryAttack();
            greedyWalk(enemyEC);
        }
    }

    private void tryAttack() throws GameActionException {
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

    private void parseECComms(int f) {
        if ((f & 1) == 1) {
            enemyEC = new MapLocation(spawnEC.x + Util.unpackSigned128(f >> 1),
                    spawnEC.y + Util.unpackSigned128(f >> 9));
            System.out.println("Politician received: " + enemyEC.toString());
            attack = true;
        }
    }

    private void greedyWalk(MapLocation loc) throws GameActionException {
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

        if (next != -1)
            rc.move(ORDINAL_DIRECTIONS[next]);
    }
}
