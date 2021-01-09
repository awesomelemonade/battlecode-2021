package metagame;

import battlecode.common.*;
import metagame.util.*;
import static metagame.util.Constants.*;

public strictfp class Slanderer implements RunnableBot {
    private RobotController rc;
    private Politician politician;
    private MapLocation nearestEC;

    public Slanderer(RobotController rc) {
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
        if (rc.getType() == RobotType.SLANDERER) {
            RobotInfo closestEnemy = Util.getClosestEnemyRobot();
            if (closestEnemy == null) {
                // if enemy EC is known, move away from it
                // try go to edge/corner
                updateNearestEC();
                if(hideAtEdge()) return;
                // lattice
                if (LatticeUtil.isLatticeLocation(rc.getLocation())) {
                    // do nothing
                } else {
                    MapLocation target = LatticeUtil.getClosestLatticeLocation(rc.getLocation());
                    if (target == null) {
                        Util.smartExplore();
                    } else {
                        Util.tryMove(target);
                    }
                }
            } else {
                tryKiteFrom(closestEnemy.getLocation());
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

    private void updateNearestEC() {
        nearestEC = SPAWNEC;
        int bestDist = nearestEC.distanceSquaredTo(Cache.MY_LOCATION);
        for(RobotInfo robot: Cache.ALL_ROBOTS) {
            if(robot.getTeam() != ENEMY_TEAM && robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                MapLocation loc = robot.location;
                int dist = loc.distanceSquaredTo(Cache.MY_LOCATION);
                if(dist < bestDist) {
                    bestDist = dist;
                    nearestEC = loc;
                }
            }
        }
    }

    public boolean tryKiteFrom(MapLocation location) throws GameActionException {
        // TODO: Use passability
        return Util.tryMoveTowards(location.directionTo(rc.getLocation()));
    }

    private double edgeScore(MapLocation loc) throws GameActionException {
        double x_dist = 9999;
        if(Cache.mapMinX != -1) x_dist = Math.min(x_dist, loc.x-Cache.mapMinX);
        if(Cache.mapMaxX != -1) x_dist = Math.min(x_dist, Cache.mapMaxX-loc.x);
        double y_dist = 9999;
        if(Cache.mapMinY != -1) y_dist = Math.min(y_dist, loc.y-Cache.mapMinY);
        if(Cache.mapMaxY != -1) y_dist = Math.min(y_dist, Cache.mapMaxY-loc.y);

        if(x_dist > 9998 && y_dist > 9998) {
            return 0;
        }
        if(x_dist == 9999) {
            return y_dist;
        } else if(y_dist == 9999) {
            return x_dist;
        } else {
            return 0.95*Math.min(x_dist, y_dist) + 0.05*Math.max(x_dist, y_dist);
        }
    }

    private double spawnScore(MapLocation loc) throws GameActionException {
        int dx = loc.x - nearestEC.x;
        int dy = loc.y - nearestEC.y;
        double dist = Math.sqrt(dx*dx + dy*dy);
        return dist;
    }

    private double hidingScore(MapLocation loc) throws GameActionException {
        if (loc.distanceSquaredTo(nearestEC) <= 9) return 9999;
        if (!LatticeUtil.isLatticeLocation(loc)) return 9999;
        MapLocation closest = Util.closestEnemyEC();
        if(closest != null) {
            int dx = loc.x - closest.x;
            int dy = loc.y - closest.y;
            double dist = Math.sqrt(dx*dx + dy*dy);
            return 95-dist;
        }
        return 0.9*edgeScore(loc) + 0.1*spawnScore(loc);
    }

    public boolean hideAtEdge() throws GameActionException {
        double bestScore = hidingScore(Cache.MY_LOCATION);
        Direction bestdir = null;
        for(Direction d: ORDINAL_DIRECTIONS) if(rc.canMove(d)) {
            MapLocation loc = Cache.MY_LOCATION.add(d);
            double score = hidingScore(loc);
            if(score < bestScore) {
                bestScore = score;
                bestdir = d;
            }
        }
        if(bestScore >= 9998) {
            // all nearby options are bad, just explore for something better
            return Util.smartExplore();
        }
        if(bestdir != null) {
            return Util.tryMove(bestdir);
        }
        return false;
    }
}
