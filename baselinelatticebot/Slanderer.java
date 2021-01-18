package baselinelatticebot;

import battlecode.common.*;
import baselinelatticebot.util.*;

public strictfp class Slanderer implements RunnableBot {
    private RobotController rc;
    private Politician politician;

    public Slanderer(RobotController rc) {
        this.rc = rc;
    }

    @Override
    public void init() throws GameActionException {

    }

    @Override
    public void turn() throws GameActionException {
        if (!rc.isReady() || Cache.TURN_COUNT <= 3) {
            return;
        }
        if (rc.getType() == RobotType.SLANDERER) {
            RobotInfo closestEnemy = Util.getClosestEnemyRobot(r -> r.getType() == RobotType.MUCKRAKER ||
                    r.getType() == RobotType.ENLIGHTENMENT_CENTER);
            MapLocation closestEnemyLocation = closestEnemy == null ? null : closestEnemy.getLocation();
            if (closestEnemyLocation == null) {
                closestEnemyLocation = UnitCommunication.closestCommunicatedEnemyToKite;
            }
            if (closestEnemyLocation != null && rc.getRoundNum() <= 150 &&
                    Pathfinder.moveDistance(Cache.MY_LOCATION, closestEnemyLocation) <= 10) {
                Util.setIndicatorDot(closestEnemyLocation, 0, 255, 0);
                tryKiteFrom(closestEnemyLocation);
            }
            if (closestEnemyLocation == null || Pathfinder.moveDistance(Cache.MY_LOCATION, closestEnemyLocation) >= 7) {
                Util.setIndicatorDot(Cache.MY_LOCATION, 0, 255, 255);
                if(hide()) {
                    return;
                }
                if(Util.smartExplore()) return;
            } else {
                Util.setIndicatorDot(closestEnemyLocation, 0, 255, 0);
                tryKiteFrom(closestEnemyLocation);
            }
        } else {
            // Camouflage
            if (politician == null) {
                politician = new Politician(rc);
                politician.init();
            }
            politician.turn();
        }
    }

    public boolean tryKiteFrom(MapLocation location) throws GameActionException {
        // TODO: Use passability
        return Util.tryMoveTowards(location.directionTo(rc.getLocation()));
    }

    private int hidingScore(MapLocation loc) {
        int dist = MapInfo.getKnownEnlightenmentCenterList(Constants.ALLY_TEAM)
                .getClosestLocationDistance(loc, 1024);
        if (dist <= 4) return 9999 - dist;
        if(!LatticeUtil.isLatticeLocation(loc)) return 9999 - dist;
        return dist;
    }

    public boolean hide() throws GameActionException {
        int bestScore = 9999;
        Direction bestdir = null;
        for(Direction d: Constants.ORDINAL_DIRECTIONS) if(rc.canMove(d)) {
            MapLocation loc = Cache.MY_LOCATION.add(d);
            int score = hidingScore(loc);
            if(score < bestScore) {
                bestScore = score;
                bestdir = d;
            }
        }
        if(bestScore >= hidingScore(Cache.MY_LOCATION)) {
            return true;
        }
        if(bestdir != null) {
            return Util.tryMove(bestdir);
        }
        return false;
    }
}
