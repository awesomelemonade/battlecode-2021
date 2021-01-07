package latticebot;

import battlecode.common.*;
import latticebot.util.Cache;
import latticebot.util.Constants;
import latticebot.util.Pathfinder;
import latticebot.util.Util;

public strictfp class Politician implements RunnableBot {
    private static RobotController rc;

    public Politician(RobotController rc) {
        this.rc = rc;
    }

    @Override
    public void init() throws GameActionException {

    }

    @Override
    public void turn() throws GameActionException {
        if (!rc.isReady()) {
            return;
        }
        if (Cache.ENEMY_ROBOTS.length == 0) {
            Util.smartExplore();
        } else {
            if (!tryEmpower()) {
                Pathfinder.execute(Util.getClosestEnemyRobot().getLocation());
            }
        }
    }

    public boolean tryEmpower() throws GameActionException {
        int actionRadiusSquared = rc.getType().actionRadiusSquared;
        // we want to find square that can target only 1 muckraker
        for (int i = 0; i < Constants.FLOOD_OFFSET_X_20.length && Clock.getBytecodesLeft() > 1200; i++) {
            MapLocation location = rc.getLocation().translate(Constants.FLOOD_OFFSET_X_20[i], Constants.FLOOD_OFFSET_Y_20[i]);
            if (i != 0 && ((!rc.onTheMap(location)) || (rc.isLocationOccupied(location)))) {
                continue;
            }
            // get closest isolated robot
            int bestDistanceSquared = actionRadiusSquared;
            RobotInfo bestRobot = null;
            boolean initial = false;
            RobotInfo[] robots = Cache.ALL_ROBOTS; // up to 80 because sight r^2 = 25
            // using some pigeonhole principle
            if (robots.length > 28) {
                robots = rc.senseNearbyRobots(location, actionRadiusSquared, null);
            }
            for (int j = robots.length; --j >= 0;) {
                RobotInfo robot = robots[j];
                if (location.isWithinDistanceSquared(robot.getLocation(), bestDistanceSquared)) {
                    int distanceSquared = location.distanceSquaredTo(robot.getLocation());
                    bestRobot = ((distanceSquared == bestDistanceSquared) && initial ? null : robot);
                    bestDistanceSquared = distanceSquared;
                    initial = true;
                }
            }
            if (bestRobot == null || bestRobot.getTeam() == Constants.ALLY_TEAM) {
                continue;
            }
            // empower at location
            if (i == 0) { // we're on the target square
                if (rc.canEmpower(bestDistanceSquared)) {
                    rc.empower(bestDistanceSquared);
                }
            } else {
                Pathfinder.execute(location);
            }
            return true;
        }
        return false;
    }
}
