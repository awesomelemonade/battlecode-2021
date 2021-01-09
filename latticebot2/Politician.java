package latticebot2;

import battlecode.common.*;
import latticebot2.util.Cache;
import latticebot2.util.Constants;
import latticebot2.util.Pathfinder;
import latticebot2.util.Util;

public strictfp class Politician implements RunnableBot {
    private static RobotController rc;
    private static int[] empower_x = { 1, 0, -1, 0, 1, -1, -1, 1, 2, 0, -2, 0, 2, 1, -1, -2, -2, -1, 1, 2, 2, -2, -2, 2,
            3, 0, -3, 0 };
    private static int[] empower_y = { 0, 1, 0, -1, 1, 1, -1, -1, 0, 2, 0, -2, 1, 2, 2, 1, -1, -2, -2, -1, 2, 2, -2, -2,
            0, 3, 0, -3 };

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
            if (tryClaimEC())
                return;
            if (goToEnemyEC()) {
                return;
            }
            Util.smartExplore();
        } else {
            if (tryEmpower()) {
                return;
            }
            if (tryClaimEC())
                return;
            Pathfinder.execute(Util.getClosestEnemyRobot().getLocation());
        }
    }

    public boolean goToEnemyEC() throws GameActionException {
        if(rc.getRoundNum()%300 <= 250) return false;
        MapLocation closest = Util.closestEnemyEC();
        if(closest == null) return false;
        return Util.tryMove(closest);
    }

    public boolean tryClaimEC() throws GameActionException {
        // if we see enemy/neutral ec, try to move closer to it
        // if can't move any closer, explode
        MapLocation bestLoc = null;
        int bestDist = 9999;
        for (RobotInfo robot : Cache.ALL_ROBOTS) {
            if (robot.getTeam() != Constants.ALLY_TEAM && robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                MapLocation loc = robot.location;
                int dist = loc.distanceSquaredTo(Cache.MY_LOCATION);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestLoc = loc;
                }
            }
        }
        if (bestLoc == null)
            return false;

        int bestNewDist = bestDist;
        Direction bestDir = null;
        for (Direction d : Constants.ORDINAL_DIRECTIONS) if(rc.canMove(d)) {
            int dist = Cache.MY_LOCATION.add(d).distanceSquaredTo(bestLoc);
            if (dist < bestNewDist) {
                bestNewDist = dist;
                bestDir = d;
            }
        }
        if (bestDir == null) { // can't get any closer
            if(bestDist > 9) return false;
            if(bestDist >= 5 && rc.senseNearbyRobots(bestDist).length >= 6) return false;
            rc.empower(bestDist);
            return true;
        } else {
            Util.tryMove(bestDir);
            return true;
        }
    }

    public boolean tryEmpower() throws GameActionException {
        int actionRadiusSquared = rc.getType().actionRadiusSquared;
        // we want to find square that can target only 1 muckraker
        for (int i = 0; i < Constants.FLOOD_OFFSET_X_20.length && Clock.getBytecodesLeft() > 1400; i++) {
            MapLocation location = rc.getLocation().translate(Constants.FLOOD_OFFSET_X_20[i],
                    Constants.FLOOD_OFFSET_Y_20[i]);
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
