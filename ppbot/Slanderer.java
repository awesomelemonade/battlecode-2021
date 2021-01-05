package ppbot;
import battlecode.common.*;
import ppbot.util.Cache;
import ppbot.util.Util;
import static ppbot.util.Constants.*;

public strictfp class Slanderer implements RunnableBot {
    private RobotController rc;

    private MapLocation cur_loc;

    public Slanderer(RobotController rc) {
        this.rc = rc;
    }

    @Override
    public void init() throws GameActionException {

    }

    @Override
    public void turn() throws GameActionException {
        cur_loc = rc.getLocation();

        if(rc.getType() == RobotType.SLANDERER) {
            if(Cache.ENEMY_ROBOTS.length > 0) {
                moveAway(closestEnemy().getLocation());
            } else {
                Util.tryRandomMove();
            }
        } else {
            attackIfWorthIt();
            Util.tryRandomMove();
        }
    }

    private RobotInfo closestEnemy() throws GameActionException {
        int closest_dist = 999999;
        RobotInfo res = null;
        for (RobotInfo robot: Cache.ENEMY_ROBOTS) {
            int dist = cur_loc.distanceSquaredTo(robot.getLocation());
            if(dist < closest_dist) {
                closest_dist = dist;
                res = robot;
            }
        }
        return res;
    }

    private void moveAway(MapLocation loc) throws GameActionException {
        int most_dist = loc.distanceSquaredTo(cur_loc);
        int next = -1;
        for (int i = 0; i < ORDINAL_DIRECTIONS.length; i++) {
            MapLocation next_loc = cur_loc.add(ORDINAL_DIRECTIONS[i]);
            int temp_dist = next_loc.distanceSquaredTo(loc);
            if (temp_dist > most_dist && rc.canMove(ORDINAL_DIRECTIONS[i])) {
                most_dist = temp_dist;
                next = i;
            }
        }

        if (next != -1)
            rc.move(ORDINAL_DIRECTIONS[next]);
    }

    private void attackIfWorthIt() throws GameActionException {
        int actionRadius = rc.getType().actionRadiusSquared;
        if(!rc.canEmpower(actionRadius)) {
            return;
        }
        int myConviction = rc.getInfluence();
        int enemyConviction = 0;
        boolean EC_in_range = false;
        for (RobotInfo robot : Cache.ENEMY_ROBOTS) {
            if (cur_loc.distanceSquaredTo(robot.location) <= actionRadius) {
                enemyConviction += robot.conviction;
                if(robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    EC_in_range = true;
                }
            }
        }
        System.out.println(Integer.toString(myConviction) + " , " + Integer.toString(enemyConviction));
        if(EC_in_range || enemyConviction * 2 >= myConviction) {
            rc.empower(actionRadius);
        }
    }
}
