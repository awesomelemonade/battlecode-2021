package latticebot;

import battlecode.common.*;
import latticebot.util.*;
import static latticebot.util.Constants.*;

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
        if (!rc.isReady()) {
            return;
        }
        if (rc.getType() == RobotType.SLANDERER) {
            RobotInfo closestEnemy = Util.getClosestEnemyRobot();
            if (closestEnemy == null) {
                // if enemy EC is known, move away from it
                MapLocation closest = Util.closestEnemyEC();
                if (closest != null) {
                    Util.tryMoveAway(closest);
                    return;
                }

                // try go to edge/corner
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

        if(x_dist == 9999) {
            return y_dist;
        } else if(y_dist == 9999) {
            return x_dist;
        } else {
            return 0.95*Math.min(x_dist, y_dist) + 0.05*Math.max(x_dist, y_dist);
        }
    }

    private double spawnScore(MapLocation loc) throws GameActionException {
        int dx = loc.x - SPAWNEC.x;
        int dy = loc.y - SPAWNEC.y;
        double dist = Math.sqrt(dx*dx + dy*dy);
        return Math.max(dist, 20*(5-dist));
    }

    private double hidingScore(MapLocation loc) throws GameActionException {
        return 0.9*edgeScore(loc) + 0.1*spawnScore(loc);
    }

    public boolean hideAtEdge() throws GameActionException {
        // if all edges are undiscovered, fail
        if(Cache.mapMinX == -1 && Cache.mapMaxX == -1 && Cache.mapMinY == -1 && Cache.mapMaxY == -1) {
            return false;
        }
        double bestScore = 9999;
        Direction bestdir = null;
        for(Direction d: ORDINAL_DIRECTIONS) if(rc.canMove(d)) {
            MapLocation loc = Cache.MY_LOCATION.add(d);
            double score = hidingScore(loc);
            if(score < bestScore) {
                bestScore = score;
                bestdir = d;
            }
        }
        if(bestScore > hidingScore(Cache.MY_LOCATION)) {
            return true;
        }
        if(bestdir != null) {
            return Util.tryMove(bestdir);
        }
        return false;
    }
}
