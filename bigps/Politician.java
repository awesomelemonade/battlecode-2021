package bigps;

import battlecode.common.*;
import bigps.util.Cache;
import bigps.util.Constants;
import bigps.util.Pathfinder;
import bigps.util.Util;

public strictfp class Politician implements RunnableBot {
    private static RobotController rc;
    private static int power;

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
        power = (int)((rc.getConviction() - 10) * rc.getEmpowerFactor(Constants.ALLY_TEAM, 0));
        if (power >= 30 && tryClaimEC()) {
            return;
        }
        if (tryEmpower()) {
            return;
        }
        if (chaseWorthwhileEnemy()) {
            return;
        }
        if (power >= 30 && Util.attackEC()) {
            return;
        }
        if (Util.smartExplore()) {
            return;
        }
    }

    private int getScore(int radiusSquared) {
        RobotInfo[] robots = rc.senseNearbyRobots(radiusSquared);
        int numUnits = robots.length;
        if(numUnits == 0) return 0;
        int damage = Math.max(0, power/numUnits);
        int numKills = 0;
        int totalConviction = 0;
        for(RobotInfo robot: robots) {
            if(robot.getTeam() != Constants.ALLY_TEAM && robot.getConviction() < damage) {
                numKills++;
                int transferredConviction = robot.getConviction();
                if(robot.getType() == RobotType.POLITICIAN || robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    transferredConviction += robot.getInfluence();
                }
                transferredConviction = Math.min(damage, transferredConviction);
                totalConviction += transferredConviction;
            }
        }
        return numKills*1000000 + totalConviction;
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
        // if can kill something, maximize the number
        int bestRadius = -1;
        int bestScore = 0;
        for(int r = 1; r <= actionRadiusSquared; r++) {
            int score = getScore(r);
            if(score > bestScore) {
                bestScore = score;
                bestRadius = r;
            }
        }
        if(bestRadius == -1) return false;

        int numKills = bestScore / 1000000;
        int convictionGotten = bestScore % 1000000;
        if(convictionGotten * 10 + 5 >= power) {
            rc.empower(bestRadius);
            return true;
        }
        return false;
    }

    private boolean chaseWorthwhileEnemy() throws GameActionException {
        int bestDist = 9999;
        MapLocation bestLoc = null;
        for(RobotInfo robot: Cache.ENEMY_ROBOTS) {
            if(power*5 >= robot.getConviction() && robot.getConviction()*5 >= power) {
                MapLocation loc = robot.location;
                int dist = Cache.MY_LOCATION.distanceSquaredTo(loc);
                if(dist < bestDist) {
                    bestDist = dist;
                    bestLoc = loc;
                }
            }
        }
        if(bestLoc == null) {
            return false;
        }
        return Util.tryMove(bestLoc);
    }
}
